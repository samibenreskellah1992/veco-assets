package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.LocationLabelDto;
import dz.vecopharm.vecoassets.entity.AssetLabelFormat;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.LocationLabel;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.LocationLabelMapper;
import dz.vecopharm.vecoassets.repository.AssetLabelFormatRepository;
import dz.vecopharm.vecoassets.repository.LocationLabelRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Generation d'etiquettes pour les locaux (Checkpoint 2 de l'evolution
 * "locaux scannables", 2026-09) - mirroir exact de {@link AssetLabelService}
 * pour l'etiquetage d'immobilisation (Phase 6) : meme PDF reel
 * ({@link LabelPdfBuilder}, reutilise tel quel via {@link
 * dz.vecopharm.vecoassets.entity.LabelPrintable}), meme tracabilite (une
 * ligne {@code location_labels} par generation, le local marque {@code
 * labeled = true}, une entree d'audit) - jamais une generation silencieuse.
 *
 * <p>Contrairement a une immobilisation, un local n'a pas d'etat "archive" :
 * seule la desactivation ({@code active = false}) existe (Checkpoint 1),
 * mais generer une etiquette pour un local desactive n'a pas de sens
 * metier (rien a afficher physiquement sur le terrain) - bloque ici comme
 * l'archivage l'est cote immobilisation.</p>
 */
@Service
public class LocationLabelService {

    private final LocationRepository locationRepository;
    private final AssetLabelFormatRepository formatRepository;
    private final LocationLabelRepository locationLabelRepository;
    private final UserRepository userRepository;
    private final LocationLabelMapper locationLabelMapper;
    private final LabelPdfBuilder labelPdfBuilder;
    private final AuditRecorder auditRecorder;

    public LocationLabelService(
            LocationRepository locationRepository,
            AssetLabelFormatRepository formatRepository,
            LocationLabelRepository locationLabelRepository,
            UserRepository userRepository,
            LocationLabelMapper locationLabelMapper,
            LabelPdfBuilder labelPdfBuilder,
            AuditRecorder auditRecorder
    ) {
        this.locationRepository = locationRepository;
        this.formatRepository = formatRepository;
        this.locationLabelRepository = locationLabelRepository;
        this.userRepository = userRepository;
        this.locationLabelMapper = locationLabelMapper;
        this.labelPdfBuilder = labelPdfBuilder;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<LocationLabelDto> historyForLocation(UUID locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Local introuvable");
        }
        return locationLabelRepository.findByLocationIdOrderByGeneratedAtDesc(locationId).stream()
                .map(locationLabelMapper::toDto)
                .toList();
    }

    @Transactional
    public byte[] generate(List<UUID> locationIds, UUID formatId) {
        AssetLabelFormat format = formatRepository.findById(formatId)
                .orElseThrow(() -> new BusinessRuleException("Format d'etiquette introuvable"));
        if (!format.isActive()) {
            throw new BusinessRuleException("Ce format d'etiquette est desactive");
        }

        List<Location> locations = locationRepository.findAllById(locationIds);
        if (locations.size() != locationIds.size()) {
            throw new BusinessRuleException("Un ou plusieurs locaux selectionnes sont introuvables");
        }
        Location inactive = locations.stream().filter(location -> !location.isActive()).findFirst().orElse(null);
        if (inactive != null) {
            throw new BusinessRuleException(
                    "Impossible de generer une etiquette pour un local desactive (" + inactive.getQrCode() + ")");
        }

        byte[] pdf = labelPdfBuilder.build(locations, format);

        User actor = currentUser();
        Instant now = Instant.now();
        for (Location location : locations) {
            LocationLabel label = new LocationLabel();
            label.setLocation(location);
            label.setFormat(format);
            label.setGeneratedBy(actor);
            label.setGeneratedAt(now);
            label = locationLabelRepository.save(label);

            location.setLabeled(true);

            auditRecorder.record(
                    AuditAction.GENERATION_ETIQUETTE,
                    "ETIQUETAGE",
                    "locations",
                    location.getId(),
                    null,
                    locationLabelMapper.toDto(label));
        }

        return pdf;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
