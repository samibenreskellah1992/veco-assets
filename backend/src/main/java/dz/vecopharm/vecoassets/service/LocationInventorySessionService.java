package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.AssetDto;
import dz.vecopharm.vecoassets.dto.LocationInventorySessionDto;
import dz.vecopharm.vecoassets.dto.LocationSessionProgressDto;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.LocationInventorySession;
import dz.vecopharm.vecoassets.entity.LocationSessionStatus;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AssetMapper;
import dz.vecopharm.vecoassets.mapper.LocationInventorySessionMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import dz.vecopharm.vecoassets.repository.InventoryScanRepository;
import dz.vecopharm.vecoassets.repository.LocationInventorySessionRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Session de scan d'inventaire pour UN local (Checkpoint 3 de l'evolution
 * "locaux scannables", 2026-09 - prompt maitre section 26, workflow
 * confirme par Sami apres livraison des Checkpoints 1/2 + tests
 * automatises). Volontairement plus legere qu'{@link InventoryCampaignService}
 * : pas de workflow BROUILLON -> ... -> CLOTURE, juste EN_COURS -> VALIDEE
 * sur un seul local a la fois - voir {@link LocationInventorySession}.
 *
 * <p>La progression est calculee en temps reel a partir des donnees
 * reelles, jamais un compteur stocke, meme discipline que {@link
 * InventoryCampaignService#progress}.</p>
 */
@Service
public class LocationInventorySessionService {

    private final LocationInventorySessionRepository sessionRepository;
    private final LocationRepository locationRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final InventoryScanRepository scanRepository;
    private final InventoryAnomalyRepository anomalyRepository;
    private final LocationInventorySessionMapper sessionMapper;
    private final AssetMapper assetMapper;
    private final AuditRecorder auditRecorder;

    public LocationInventorySessionService(
            LocationInventorySessionRepository sessionRepository,
            LocationRepository locationRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            InventoryScanRepository scanRepository,
            InventoryAnomalyRepository anomalyRepository,
            LocationInventorySessionMapper sessionMapper,
            AssetMapper assetMapper,
            AuditRecorder auditRecorder
    ) {
        this.sessionRepository = sessionRepository;
        this.locationRepository = locationRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.scanRepository = scanRepository;
        this.anomalyRepository = anomalyRepository;
        this.sessionMapper = sessionMapper;
        this.assetMapper = assetMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<LocationInventorySessionDto> list(UUID locationId) {
        List<LocationInventorySession> sessions = locationId != null
                ? sessionRepository.findByLocationIdOrderByOpenedAtDesc(locationId)
                : sessionRepository.findAllByOrderByOpenedAtDesc();
        return sessions.stream().map(sessionMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LocationInventorySessionDto findById(UUID id) {
        return sessionMapper.toDto(getOrThrow(id));
    }

    /**
     * Ouvre une session de scan sur un local. Un seul local a la fois est
     * scanne (pas de perimetre site/zone comme une campagne) et une seule
     * session {@code EN_COURS} est autorisee par local - verifie ici pour
     * un message d'erreur clair, garanti en base par l'index unique
     * partiel V17 en cas de double ouverture concurrente.
     */
    @Transactional
    public LocationInventorySessionDto open(UUID locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Local introuvable"));
        if (!location.isActive()) {
            throw new BusinessRuleException("Ce local est desactive - aucune session de scan ne peut y etre ouverte");
        }
        if (sessionRepository.existsByLocationIdAndStatus(locationId, LocationSessionStatus.EN_COURS)) {
            throw new BusinessRuleException("Une session de scan est deja en cours pour ce local");
        }

        LocationInventorySession session = new LocationInventorySession();
        session.setLocation(location);
        session.setOpenedBy(currentUser());
        session.setOpenedAt(Instant.now());
        session.setStatus(LocationSessionStatus.EN_COURS);
        session = sessionRepository.save(session);
        auditRecorder.record(AuditAction.CREATION, "INVENTAIRE", "location_inventory_sessions", session.getId(), null, sessionMapper.toDto(session));
        return sessionMapper.toDto(session);
    }

    /**
     * Cloture et valide une session de scan. Met egalement a jour {@code
     * Location.lastInventoryAt} du local concerne - meme champ que celui
     * deja porte par {@link Location} depuis le Checkpoint 1, jusqu'ici
     * jamais ecrit faute de workflow d'inventaire par local.
     */
    @Transactional
    public LocationInventorySessionDto validate(UUID id) {
        LocationInventorySession session = getOrThrow(id);
        if (session.getStatus() != LocationSessionStatus.EN_COURS) {
            throw new BusinessRuleException(
                    "Cette session est deja " + session.getStatus() + " - seule une session EN_COURS peut etre validee");
        }
        Instant now = Instant.now();
        session.setStatus(LocationSessionStatus.VALIDEE);
        session.setValidatedBy(currentUser());
        session.setValidatedAt(now);
        session = sessionRepository.save(session);

        Location location = session.getLocation();
        location.setLastInventoryAt(now);
        locationRepository.save(location);

        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "INVENTAIRE", "location_inventory_sessions", session.getId(),
                LocationSessionStatus.EN_COURS, LocationSessionStatus.VALIDEE);
        return sessionMapper.toDto(session);
    }

    /**
     * Progression calculee en croisant les scans de la session avec les
     * immobilisations reellement attendues dans ce local (celles dont
     * {@code Asset.location} pointe vers ce meme local) - meme discipline
     * que {@code InventoryCampaignService#progress} : un scan hors
     * perimetre (anomalie MAUVAISE_LOCALISATION) ne compte jamais comme
     * "immobilisation attendue scannee".
     */
    @Transactional(readOnly = true)
    public LocationSessionProgressDto progress(UUID id) {
        LocationInventorySession session = getOrThrow(id);
        Set<UUID> scopeIds = scopeAssetIds(session.getLocation().getId());
        Set<UUID> scannedIds = scanRepository.findDistinctAssetIdsByLocationSessionId(id);
        Set<UUID> presentIds = scanRepository.findDistinctPresentAssetIdsByLocationSessionId(id);

        long totalInScope = scopeIds.size();
        long scannedInScope = scannedIds.stream().filter(scopeIds::contains).count();
        long presentInScope = presentIds.stream().filter(scopeIds::contains).count();
        long anomalies = anomalyRepository.countByLocationSessionId(id);
        long remaining = Math.max(0, totalInScope - scannedInScope);
        double percent = totalInScope == 0 ? 0.0 : Math.round((scannedInScope * 10000.0) / totalInScope) / 100.0;
        return new LocationSessionProgressDto(id, totalInScope, scannedInScope, presentInScope, anomalies, remaining, percent);
    }

    /** Immobilisations attendues dans le local de la session mais pas encore scannees. */
    @Transactional(readOnly = true)
    public List<AssetDto> pendingAssets(UUID id) {
        LocationInventorySession session = getOrThrow(id);
        Set<UUID> scannedIds = scanRepository.findDistinctAssetIdsByLocationSessionId(id);
        return assetRepository.findByLocationIdAndDeletedFalse(session.getLocation().getId()).stream()
                .filter(asset -> !scannedIds.contains(asset.getId()))
                .map(assetMapper::toDto)
                .toList();
    }

    private Set<UUID> scopeAssetIds(UUID locationId) {
        return assetRepository.findByLocationIdAndDeletedFalse(locationId).stream()
                .map(Asset::getId)
                .collect(Collectors.toSet());
    }

    LocationInventorySession getOrThrow(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session de scan de local introuvable"));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
