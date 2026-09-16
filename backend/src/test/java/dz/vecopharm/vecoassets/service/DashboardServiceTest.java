package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.AuditLogDto;
import dz.vecopharm.vecoassets.dto.CountByLabelDto;
import dz.vecopharm.vecoassets.dto.DashboardDto;
import dz.vecopharm.vecoassets.entity.AnomalyStatus;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito) du tableau de bord (prompt maitre Phase 9) :
 * chaque chiffre doit etre recalcule correctement depuis la liste
 * d'immobilisations/anomalies simulee, jamais suppose - meme esprit que la
 * verification SQL de la Phase 9, mais executable ici (contrairement a
 * cette derniere, cf. limites d'environnement documentees dans
 * docs/ROADMAP.md section 13).
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private InventoryAnomalyRepository anomalyRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void get_computesTotalsAndStatusBucketsFromTheActiveFleet() {
        Site site = siteNamed("Siege");
        List<Asset> assets = List.of(
                assetOf(site, AssetStatus.EN_STOCK, true, true, new BigDecimal("1000")),
                assetOf(site, AssetStatus.EN_SERVICE, false, true, new BigDecimal("2000")),
                assetOf(site, AssetStatus.EN_MAINTENANCE, false, false, null),
                assetOf(site, AssetStatus.REFORME, true, true, new BigDecimal("500")),
                assetOf(site, AssetStatus.SORTI, false, false, null)
        );
        when(assetRepository.findByDeletedFalse()).thenReturn(assets);
        when(anomalyRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(auditLogService.recent(10)).thenReturn(List.of());

        DashboardDto dashboard = dashboardService.get();

        assertThat(dashboard.totalAssets()).isEqualTo(5);
        assertThat(dashboard.labeledAssets()).isEqualTo(2);
        assertThat(dashboard.unlabeledAssets()).isEqualTo(3);
        assertThat(dashboard.inventoriedAssets()).isEqualTo(3);
        assertThat(dashboard.notInventoriedAssets()).isEqualTo(2);
        assertThat(dashboard.inStock()).isEqualTo(1);
        assertThat(dashboard.inService()).isEqualTo(1);
        assertThat(dashboard.inMaintenance()).isEqualTo(1);
        assertThat(dashboard.reformed()).isEqualTo(1);
        assertThat(dashboard.exited()).isEqualTo(1);
        // 1000 + 2000 + 500, les deux valeurs nulles sont ignorees plutot que de faire echouer la somme.
        assertThat(dashboard.totalAcquisitionValue()).isEqualByComparingTo("3500");
    }

    @Test
    void get_countsOnlyNouvelleAndEnCoursAnomaliesAsOpen() {
        when(assetRepository.findByDeletedFalse()).thenReturn(List.of());
        when(anomalyRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                anomalyOf(AnomalyStatus.NOUVELLE),
                anomalyOf(AnomalyStatus.EN_COURS),
                anomalyOf(AnomalyStatus.RESOLUE),
                anomalyOf(AnomalyStatus.REJETEE)
        ));
        when(auditLogService.recent(10)).thenReturn(List.of());

        DashboardDto dashboard = dashboardService.get();

        assertThat(dashboard.openAnomalies()).isEqualTo(2);
    }

    @Test
    void get_groupsBySiteSortedByCountDescendingThenAlphabetically() {
        Site a = siteNamed("Alger");
        Site b = siteNamed("Blida");
        Site c = siteNamed("Constantine");
        List<Asset> assets = List.of(
                assetOf(a, AssetStatus.EN_STOCK, false, false, null),
                assetOf(b, AssetStatus.EN_STOCK, false, false, null),
                assetOf(b, AssetStatus.EN_STOCK, false, false, null),
                assetOf(c, AssetStatus.EN_STOCK, false, false, null),
                assetOf(c, AssetStatus.EN_STOCK, false, false, null)
        );
        when(assetRepository.findByDeletedFalse()).thenReturn(assets);
        when(anomalyRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(auditLogService.recent(10)).thenReturn(List.of());

        DashboardDto dashboard = dashboardService.get();

        // Blida et Constantine sont a egalite (2) : ordre alphabetique entre elles, toutes deux avant Alger (1).
        assertThat(dashboard.bySite()).extracting(CountByLabelDto::label).containsExactly("Blida", "Constantine", "Alger");
        assertThat(dashboard.bySite()).extracting(CountByLabelDto::count).containsExactly(2L, 2L, 1L);
    }

    @Test
    void get_delegatesRecentActivityToAuditLogServiceWithTenEntryLimit() {
        when(assetRepository.findByDeletedFalse()).thenReturn(List.of());
        when(anomalyRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        List<AuditLogDto> recent = List.of(new AuditLogDto(
                UUID.randomUUID(), "Amine Test", "CONNEXION", "AUTH", "users", UUID.randomUUID(), null, null, "127.0.0.1", Instant.now()));
        when(auditLogService.recent(10)).thenReturn(recent);

        DashboardDto dashboard = dashboardService.get();

        verify(auditLogService).recent(10);
        assertThat(dashboard.recentActivity()).isEqualTo(recent);
    }

    // --- Helpers -------------------------------------------------

    private static Site siteNamed(String name) {
        Site site = new Site();
        site.setId(UUID.randomUUID());
        site.setName(name);
        return site;
    }

    private static Asset assetOf(Site site, AssetStatus status, boolean labeled, boolean inventoried, BigDecimal value) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetCode("VECO-IMM-" + UUID.randomUUID().toString().substring(0, 6));
        asset.setSite(site);
        AssetCategory category = new AssetCategory();
        category.setId(UUID.randomUUID());
        category.setName("Informatique");
        asset.setCategory(category);
        asset.setCondition(AssetCondition.BON);
        asset.setStatus(status);
        asset.setLabeled(labeled);
        asset.setLastInventoryAt(inventoried ? Instant.now() : null);
        asset.setAcquisitionValue(value);
        asset.setDeleted(false);
        return asset;
    }

    private static InventoryAnomaly anomalyOf(AnomalyStatus status) {
        InventoryAnomaly anomaly = new InventoryAnomaly();
        anomaly.setId(UUID.randomUUID());
        InventoryCampaign campaign = new InventoryCampaign();
        campaign.setId(UUID.randomUUID());
        anomaly.setCampaign(campaign);
        anomaly.setStatus(status);
        return anomaly;
    }
}
