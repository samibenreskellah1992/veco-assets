package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.AssetDto;
import dz.vecopharm.vecoassets.dto.CampaignProgressDto;
import dz.vecopharm.vecoassets.dto.InventoryCampaignDto;
import dz.vecopharm.vecoassets.dto.InventoryCampaignRequest;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.CampaignStatus;
import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.entity.Zone;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AssetMapper;
import dz.vecopharm.vecoassets.mapper.InventoryCampaignMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import dz.vecopharm.vecoassets.repository.InventoryCampaignRepository;
import dz.vecopharm.vecoassets.repository.InventoryScanRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import dz.vecopharm.vecoassets.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Campagnes d'inventaire (prompt maitre Phase 7 / sections 14-16). Le
 * workflow de statut est strictement lineaire - {@link CampaignStatus} est
 * declare dans cet ordre precis et {@link #transition} n'autorise jamais de
 * sauter une etape ni de revenir en arriere, voir {@link #transition}.
 */
@Service
public class InventoryCampaignService {

    private final InventoryCampaignRepository campaignRepository;
    private final SiteRepository siteRepository;
    private final ZoneRepository zoneRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final InventoryScanRepository scanRepository;
    private final InventoryAnomalyRepository anomalyRepository;
    private final InventoryCampaignMapper campaignMapper;
    private final AssetMapper assetMapper;
    private final AuditRecorder auditRecorder;

    public InventoryCampaignService(
            InventoryCampaignRepository campaignRepository,
            SiteRepository siteRepository,
            ZoneRepository zoneRepository,
            UserRepository userRepository,
            AssetRepository assetRepository,
            InventoryScanRepository scanRepository,
            InventoryAnomalyRepository anomalyRepository,
            InventoryCampaignMapper campaignMapper,
            AssetMapper assetMapper,
            AuditRecorder auditRecorder
    ) {
        this.campaignRepository = campaignRepository;
        this.siteRepository = siteRepository;
        this.zoneRepository = zoneRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.scanRepository = scanRepository;
        this.anomalyRepository = anomalyRepository;
        this.campaignMapper = campaignMapper;
        this.assetMapper = assetMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<InventoryCampaignDto> list(UUID siteId, CampaignStatus status) {
        return campaignRepository.findAllByOrderByStartDateDesc().stream()
                .filter(c -> siteId == null || c.getSite().getId().equals(siteId))
                .filter(c -> status == null || c.getStatus() == status)
                .map(campaignMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryCampaignDto findById(UUID id) {
        return campaignMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public InventoryCampaignDto create(InventoryCampaignRequest request) {
        InventoryCampaign campaign = new InventoryCampaign();
        applyFields(campaign, request);
        campaign.setStatus(CampaignStatus.BROUILLON);
        campaign = campaignRepository.save(campaign);
        auditRecorder.record(AuditAction.CREATION, "INVENTAIRE", "inventory_campaigns", campaign.getId(), null, campaignMapper.toDto(campaign));
        return campaignMapper.toDto(campaign);
    }

    @Transactional
    public InventoryCampaignDto update(UUID id, InventoryCampaignRequest request) {
        InventoryCampaign campaign = getOrThrow(id);
        if (campaign.getStatus() != CampaignStatus.BROUILLON && campaign.getStatus() != CampaignStatus.EN_PREPARATION) {
            throw new BusinessRuleException(
                    "Une campagne ne peut plus etre modifiee une fois passee en EN_COURS (statut actuel : " + campaign.getStatus() + ")");
        }
        InventoryCampaignDto before = campaignMapper.toDto(campaign);
        applyFields(campaign, request);
        campaign = campaignRepository.save(campaign);
        auditRecorder.record(AuditAction.MODIFICATION, "INVENTAIRE", "inventory_campaigns", campaign.getId(), before, campaignMapper.toDto(campaign));
        return campaignMapper.toDto(campaign);
    }

    /**
     * Avance strictement d'une etape dans le workflow {@code BROUILLON ->
     * EN_PREPARATION -> EN_COURS -> TERMINE -> VALIDE -> CLOTURE} (prompt
     * maitre Phase 7). {@link CampaignStatus} est declare dans cet ordre
     * precis, donc {@code target.ordinal()} doit toujours valoir {@code
     * current.ordinal() + 1} - jamais de saut d'etape, jamais de retour en
     * arriere. Les permissions differentes entre "faire progresser" et
     * "valider/cloturer" sont appliquees au niveau controller.
     */
    @Transactional
    public InventoryCampaignDto transition(UUID id, CampaignStatus target) {
        InventoryCampaign campaign = getOrThrow(id);
        CampaignStatus current = campaign.getStatus();
        if (target.ordinal() != current.ordinal() + 1) {
            throw new BusinessRuleException(
                    "Transition invalide : une campagne " + current + " ne peut passer qu'a l'etape suivante du workflow, pas directement a " + target);
        }
        campaign.setStatus(target);
        campaign = campaignRepository.save(campaign);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "INVENTAIRE", "inventory_campaigns", campaign.getId(), current, target);
        return campaignMapper.toDto(campaign);
    }

    /**
     * Progression calculee en croisant les scans avec le perimetre reel de
     * la campagne : un scan sur une immobilisation hors perimetre (anomalie
     * MAUVAISE_LOCALISATION, {@link InventoryScanService}) ne doit jamais
     * compter comme "immobilisation attendue scannee" - sinon le compteur
     * restant peut afficher 0 alors qu'une immobilisation du perimetre n'a
     * en realite jamais ete vue. Bug trouve et corrige pendant la
     * verification SQL de cette phase (voir docs/ROADMAP.md section 13).
     */
    @Transactional(readOnly = true)
    public CampaignProgressDto progress(UUID id) {
        InventoryCampaign campaign = getOrThrow(id);
        Set<UUID> scopeIds = scopeAssetIds(campaign);
        Set<UUID> scannedIds = scanRepository.findDistinctAssetIdsByCampaignId(id);
        Set<UUID> presentIds = scanRepository.findDistinctPresentAssetIdsByCampaignId(id);

        long totalInScope = scopeIds.size();
        long scannedInScope = scannedIds.stream().filter(scopeIds::contains).count();
        long presentInScope = presentIds.stream().filter(scopeIds::contains).count();
        long anomalies = anomalyRepository.countByCampaignId(id);
        long remaining = Math.max(0, totalInScope - scannedInScope);
        double percent = totalInScope == 0 ? 0.0 : Math.round((scannedInScope * 10000.0) / totalInScope) / 100.0;
        return new CampaignProgressDto(id, totalInScope, scannedInScope, presentInScope, anomalies, remaining, percent);
    }

    /** Immobilisations du perimetre de la campagne pas encore scannees (aucun resultat, PRESENT ou ANOMALIE). */
    @Transactional(readOnly = true)
    public List<AssetDto> pendingAssets(UUID id) {
        InventoryCampaign campaign = getOrThrow(id);
        Set<UUID> scannedIds = scanRepository.findDistinctAssetIdsByCampaignId(id);
        return scopeAssets(campaign).stream()
                .filter(asset -> !scannedIds.contains(asset.getId()))
                .map(assetMapper::toDto)
                .toList();
    }

    /** Immobilisations non archivees appartenant au perimetre (site, et zone si la campagne en cible une) de la campagne. */
    private List<Asset> scopeAssets(InventoryCampaign campaign) {
        return campaign.getZone() != null
                ? assetRepository.findBySiteIdAndZoneIdAndDeletedFalse(campaign.getSite().getId(), campaign.getZone().getId())
                : assetRepository.findBySiteIdAndDeletedFalse(campaign.getSite().getId());
    }

    private Set<UUID> scopeAssetIds(InventoryCampaign campaign) {
        return scopeAssets(campaign).stream().map(Asset::getId).collect(Collectors.toSet());
    }

    private void applyFields(InventoryCampaign campaign, InventoryCampaignRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("La date de fin ne peut pas etre anterieure a la date de debut");
        }
        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> new BusinessRuleException("Site introuvable"));
        Zone zone = null;
        if (request.zoneId() != null) {
            zone = zoneRepository.findById(request.zoneId())
                    .orElseThrow(() -> new BusinessRuleException("Zone introuvable"));
            if (!zone.getFloor().getBuilding().getSite().getId().equals(site.getId())) {
                throw new BusinessRuleException("La zone selectionnee n'appartient pas au site selectionne");
            }
        }
        User responsible = null;
        if (request.responsibleUserId() != null) {
            responsible = userRepository.findById(request.responsibleUserId())
                    .orElseThrow(() -> new BusinessRuleException("Utilisateur responsable introuvable"));
        }
        campaign.setName(request.name().trim());
        campaign.setSite(site);
        campaign.setZone(zone);
        campaign.setResponsibleUser(responsible);
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
    }

    InventoryCampaign getOrThrow(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campagne d'inventaire introuvable"));
    }
}
