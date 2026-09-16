package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.ScanRequest;
import dz.vecopharm.vecoassets.dto.ScanResponse;
import dz.vecopharm.vecoassets.entity.AnomalyType;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.CampaignStatus;
import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import dz.vecopharm.vecoassets.entity.InventoryScan;
import dz.vecopharm.vecoassets.entity.ScanResult;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.mapper.InventoryAnomalyMapper;
import dz.vecopharm.vecoassets.mapper.InventoryScanMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import dz.vecopharm.vecoassets.repository.InventoryScanRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito) des 3 issues possibles d'un scan d'inventaire
 * (prompt maitre Phase 7 / section 15) : code non reconnu (NON_REFERENCEE),
 * immobilisation reconnue mais hors perimetre (MAUVAISE_LOCALISATION,
 * quel que soit le resultat demande par le client), et immobilisation dans
 * le perimetre (PRESENT ou ANOMALIE declaree). Verifie aussi la couverture
 * de l'audit trail corrigee Phase 10 : la creation d'une anomalie porte
 * desormais toujours sa propre ligne d'audit, en plus de celle du scan.
 */
@ExtendWith(MockitoExtension.class)
class InventoryScanServiceTest {

    @Mock
    private InventoryCampaignService campaignService;
    @Mock
    private InventoryScanRepository scanRepository;
    @Mock
    private InventoryAnomalyRepository anomalyRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryScanMapper scanMapper;
    @Mock
    private InventoryAnomalyMapper anomalyMapper;
    @Mock
    private AuditRecorder auditRecorder;

    @InjectMocks
    private InventoryScanService inventoryScanService;

    private UUID campaignId;
    private Site site;
    private InventoryCampaign campaign;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        site = new Site();
        site.setId(UUID.randomUUID());

        campaign = new InventoryCampaign();
        campaign.setId(campaignId);
        campaign.setSite(site);
        campaign.setStatus(CampaignStatus.EN_COURS);

        lenient().when(anomalyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(scanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void scan_rejectsWhenCampaignNotEnCours() {
        campaign.setStatus(CampaignStatus.BROUILLON);
        when(campaignService.getOrThrow(campaignId)).thenReturn(campaign);

        assertThatThrownBy(() -> inventoryScanService.scan(campaignId, new ScanRequest("VECO-IMM-000001", ScanResult.PRESENT, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("EN_COURS");
    }

    @Test
    void scan_unknownCode_createsNonReferencedAnomalyWithoutAScanRow() {
        when(campaignService.getOrThrow(campaignId)).thenReturn(campaign);
        when(assetRepository.findByAssetCode("INCONNU")).thenReturn(Optional.empty());

        ScanResponse response = inventoryScanService.scan(campaignId, new ScanRequest("INCONNU", ScanResult.PRESENT, null, null));

        assertThat(response.assetRecognized()).isFalse();
        assertThat(response.scan()).isNull();

        ArgumentCaptor<InventoryAnomaly> captor = ArgumentCaptor.forClass(InventoryAnomaly.class);
        verify(anomalyRepository).save(captor.capture());
        assertThat(captor.getValue().getAnomalyType()).isEqualTo(AnomalyType.NON_REFERENCEE);
        assertThat(captor.getValue().getAsset()).isNull();

        verify(scanRepository, times(0)).save(any());
        verify(auditRecorder, times(1)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void scan_archivedAssetIsTreatedAsUnknownCode() {
        Asset archived = new Asset();
        archived.setId(UUID.randomUUID());
        archived.setAssetCode("VECO-IMM-000002");
        archived.setDeleted(true);
        when(campaignService.getOrThrow(campaignId)).thenReturn(campaign);
        when(assetRepository.findByAssetCode("VECO-IMM-000002")).thenReturn(Optional.of(archived));

        ScanResponse response = inventoryScanService.scan(campaignId, new ScanRequest("VECO-IMM-000002", ScanResult.PRESENT, null, null));

        assertThat(response.assetRecognized()).isFalse();
    }

    @Test
    void scan_outOfScope_forcesAnomalyRegardlessOfRequestedResultAndAuditsScanAndAnomaly() {
        Site otherSite = new Site();
        otherSite.setId(UUID.randomUUID());
        Asset asset = assetIn(otherSite);
        when(campaignService.getOrThrow(campaignId)).thenReturn(campaign);
        when(assetRepository.findByAssetCode(asset.getAssetCode())).thenReturn(Optional.of(asset));

        // Le client demande PRESENT, mais l'immobilisation est hors perimetre : forcee en ANOMALIE.
        ScanResponse response = inventoryScanService.scan(campaignId, new ScanRequest(asset.getAssetCode(), ScanResult.PRESENT, null, null));

        assertThat(response.assetRecognized()).isTrue();
        ArgumentCaptor<InventoryScan> scanCaptor = ArgumentCaptor.forClass(InventoryScan.class);
        verify(scanRepository).save(scanCaptor.capture());
        assertThat(scanCaptor.getValue().getResult()).isEqualTo(ScanResult.ANOMALIE);

        ArgumentCaptor<InventoryAnomaly> anomalyCaptor = ArgumentCaptor.forClass(InventoryAnomaly.class);
        verify(anomalyRepository).save(anomalyCaptor.capture());
        assertThat(anomalyCaptor.getValue().getAnomalyType()).isEqualTo(AnomalyType.MAUVAISE_LOCALISATION);

        // Une ligne d'audit pour le scan, une pour l'anomalie qu'il a declenchee (fix Phase 10).
        verify(auditRecorder, times(2)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void scan_inScopePresent_marksLastInventoryAtAndCreatesNoAnomaly() {
        Asset asset = assetIn(site);
        when(campaignService.getOrThrow(campaignId)).thenReturn(campaign);
        when(assetRepository.findByAssetCode(asset.getAssetCode())).thenReturn(Optional.of(asset));

        ScanResponse response = inventoryScanService.scan(campaignId, new ScanRequest(asset.getAssetCode(), ScanResult.PRESENT, null, null));

        assertThat(response.assetRecognized()).isTrue();
        assertThat(response.anomaly()).isNull();
        assertThat(asset.getLastInventoryAt()).isNotNull();
        verify(anomalyRepository, times(0)).save(any());
        verify(auditRecorder, times(1)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void scan_inScopeAnomalie_usesRequestedAnomalyTypeAndAuditsScanAndAnomaly() {
        Asset asset = assetIn(site);
        when(campaignService.getOrThrow(campaignId)).thenReturn(campaign);
        when(assetRepository.findByAssetCode(asset.getAssetCode())).thenReturn(Optional.of(asset));

        inventoryScanService.scan(campaignId, new ScanRequest(asset.getAssetCode(), ScanResult.ANOMALIE, AnomalyType.NUMERO_SERIE_DIFFERENT, "numero different"));

        ArgumentCaptor<InventoryAnomaly> anomalyCaptor = ArgumentCaptor.forClass(InventoryAnomaly.class);
        verify(anomalyRepository).save(anomalyCaptor.capture());
        assertThat(anomalyCaptor.getValue().getAnomalyType()).isEqualTo(AnomalyType.NUMERO_SERIE_DIFFERENT);
        verify(auditRecorder, times(2)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void scan_inScopeAnomalieWithoutExplicitType_defaultsToAutre() {
        Asset asset = assetIn(site);
        when(campaignService.getOrThrow(campaignId)).thenReturn(campaign);
        when(assetRepository.findByAssetCode(asset.getAssetCode())).thenReturn(Optional.of(asset));

        inventoryScanService.scan(campaignId, new ScanRequest(asset.getAssetCode(), ScanResult.ANOMALIE, null, null));

        ArgumentCaptor<InventoryAnomaly> anomalyCaptor = ArgumentCaptor.forClass(InventoryAnomaly.class);
        verify(anomalyRepository).save(anomalyCaptor.capture());
        assertThat(anomalyCaptor.getValue().getAnomalyType()).isEqualTo(AnomalyType.AUTRE);
    }

    private Asset assetIn(Site assetSite) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetCode("VECO-IMM-" + UUID.randomUUID().toString().substring(0, 6));
        asset.setSite(assetSite);
        asset.setDeleted(false);
        return asset;
    }
}
