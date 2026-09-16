package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.InventoryScanDto;
import dz.vecopharm.vecoassets.dto.ScanRequest;
import dz.vecopharm.vecoassets.dto.ScanResponse;
import dz.vecopharm.vecoassets.entity.AnomalyType;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.CampaignStatus;
import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import dz.vecopharm.vecoassets.entity.InventoryScan;
import dz.vecopharm.vecoassets.entity.ScanResult;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.mapper.InventoryAnomalyMapper;
import dz.vecopharm.vecoassets.mapper.InventoryScanMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import dz.vecopharm.vecoassets.repository.InventoryScanRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Scan d'inventaire (prompt maitre Phase 7 / section 15) : scan QR ->
 * verification d'appartenance a la campagne -> confirmation de presence ou
 * declaration d'anomalie. {@code assetCode} est la seule donnee portee par
 * le QR code (section 13) - la resolution complete de l'immobilisation se
 * fait toujours cote backend, jamais a partir de donnees envoyees par le
 * client au-dela de ce code.
 */
@Service
public class InventoryScanService {

    private final InventoryCampaignService campaignService;
    private final InventoryScanRepository scanRepository;
    private final InventoryAnomalyRepository anomalyRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final InventoryScanMapper scanMapper;
    private final InventoryAnomalyMapper anomalyMapper;
    private final AuditRecorder auditRecorder;

    public InventoryScanService(
            InventoryCampaignService campaignService,
            InventoryScanRepository scanRepository,
            InventoryAnomalyRepository anomalyRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            InventoryScanMapper scanMapper,
            InventoryAnomalyMapper anomalyMapper,
            AuditRecorder auditRecorder
    ) {
        this.campaignService = campaignService;
        this.scanRepository = scanRepository;
        this.anomalyRepository = anomalyRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.scanMapper = scanMapper;
        this.anomalyMapper = anomalyMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<InventoryScanDto> historyForCampaign(UUID campaignId) {
        return scanRepository.findByCampaignIdOrderByScannedAtDesc(campaignId).stream()
                .map(scanMapper::toDto)
                .toList();
    }

    /** Historique de scan d'une immobilisation, toutes campagnes confondues - alimente l'onglet "Inventaire" de sa fiche. */
    @Transactional(readOnly = true)
    public List<InventoryScanDto> historyForAsset(UUID assetId) {
        return scanRepository.findByAssetIdOrderByScannedAtDesc(assetId).stream()
                .map(scanMapper::toDto)
                .toList();
    }

    @Transactional
    public ScanResponse scan(UUID campaignId, ScanRequest request) {
        InventoryCampaign campaign = campaignService.getOrThrow(campaignId);
        // Regle explicite (prompt maitre Phase 7) : une campagne cloturee
        // n'accepte plus de scans. Restreint en pratique a EN_COURS : une
        // campagne pas encore demarree (BROUILLON/EN_PREPARATION) ou deja
        // au-dela de l'execution (TERMINE/VALIDE/CLOTURE) n'en accepte pas
        // non plus - le scan n'a de sens que pendant la phase d'execution.
        if (campaign.getStatus() != CampaignStatus.EN_COURS) {
            throw new BusinessRuleException(
                    "Cette campagne n'accepte pas de scan dans son statut actuel (" + campaign.getStatus()
                            + ") - seule une campagne EN_COURS peut etre scannee");
        }

        User scannedBy = currentUser();
        Instant now = Instant.now();
        String code = request.assetCode().trim();

        Optional<Asset> maybeAsset = assetRepository.findByAssetCode(code).filter(a -> !a.isDeleted());
        if (maybeAsset.isEmpty()) {
            InventoryAnomaly anomaly = new InventoryAnomaly();
            anomaly.setCampaign(campaign);
            anomaly.setAsset(null);
            anomaly.setScan(null);
            anomaly.setAnomalyType(AnomalyType.NON_REFERENCEE);
            anomaly.setDescription(("Code scanne inconnu ou immobilisation archivee : " + code
                    + (hasText(request.comment()) ? " - " + request.comment() : "")).trim());
            anomaly.setReportedBy(scannedBy);
            anomaly = anomalyRepository.save(anomaly);
            auditRecorder.record(AuditAction.INVENTAIRE, "INVENTAIRE", "inventory_anomalies", anomaly.getId(), null, anomalyMapper.toDto(anomaly));
            return new ScanResponse(false, null, anomalyMapper.toDto(anomaly));
        }

        Asset asset = maybeAsset.get();
        boolean inScope = campaign.getSite().getId().equals(asset.getSite().getId())
                && (campaign.getZone() == null || (asset.getZone() != null && campaign.getZone().getId().equals(asset.getZone().getId())));

        InventoryScan scan = new InventoryScan();
        scan.setCampaign(campaign);
        scan.setAsset(asset);
        scan.setScannedBy(scannedBy);
        scan.setScannedAt(now);

        if (!inScope) {
            // L'immobilisation existe mais ne correspond pas au perimetre de
            // la campagne (site/zone) : c'est une anomalie de localisation,
            // quel que soit le resultat demande par le client.
            scan.setResult(ScanResult.ANOMALIE);
            scan.setComment(request.comment());
            scan = scanRepository.save(scan);
            InventoryAnomaly anomaly = createAnomaly(campaign, asset, scan, AnomalyType.MAUVAISE_LOCALISATION,
                    "Immobilisation scannee hors du perimetre de la campagne (site/zone attendu different)"
                            + (hasText(request.comment()) ? " - " + request.comment() : ""),
                    scannedBy);
            auditRecorder.record(AuditAction.INVENTAIRE, "INVENTAIRE", "assets", asset.getId(), null, scanMapper.toDto(scan));
            return new ScanResponse(true, scanMapper.toDto(scan), anomalyMapper.toDto(anomaly));
        }

        scan.setResult(request.result());
        scan.setComment(request.comment());
        scan = scanRepository.save(scan);
        asset.setLastInventoryAt(now);

        InventoryAnomaly anomaly = null;
        if (request.result() == ScanResult.ANOMALIE) {
            AnomalyType type = request.anomalyType() != null ? request.anomalyType() : AnomalyType.AUTRE;
            anomaly = createAnomaly(campaign, asset, scan, type, request.comment(), scannedBy);
        }

        auditRecorder.record(AuditAction.INVENTAIRE, "INVENTAIRE", "assets", asset.getId(), null, scanMapper.toDto(scan));
        return new ScanResponse(true, scanMapper.toDto(scan), anomaly != null ? anomalyMapper.toDto(anomaly) : null);
    }

    private InventoryAnomaly createAnomaly(InventoryCampaign campaign, Asset asset, InventoryScan scan, AnomalyType type, String description, User reportedBy) {
        InventoryAnomaly anomaly = new InventoryAnomaly();
        anomaly.setCampaign(campaign);
        anomaly.setAsset(asset);
        anomaly.setScan(scan);
        anomaly.setAnomalyType(type);
        anomaly.setDescription(description);
        anomaly.setReportedBy(reportedBy);
        return anomalyRepository.save(anomaly);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
