package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.ReportFilter;
import dz.vecopharm.vecoassets.dto.ReportResultDto;
import dz.vecopharm.vecoassets.dto.ReportType;
import dz.vecopharm.vecoassets.entity.AnomalyStatus;
import dz.vecopharm.vecoassets.entity.AnomalyType;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import dz.vecopharm.vecoassets.entity.AssetMovement;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import dz.vecopharm.vecoassets.entity.MovementStatus;
import dz.vecopharm.vecoassets.entity.MovementType;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.repository.AssetMovementRepository;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito) des 11 {@link ReportType} de {@link ReportService}
 * (prompt maitre Phase 9) : logique d'agregation (regroupement, sommes) et,
 * en particulier, le cas limite REFORMES documente dans le code (une
 * immobilisation reformee peut n'avoir aucun mouvement REFORME associe -
 * confirme par la verification SQL de la Phase 9, voir le commentaire de
 * {@code ReportService.reformes}). Le detail des lignes formatees (dates,
 * montants) n'est pas reteste ici colonne par colonne : on verifie la
 * structure et les valeurs qui comptent pour la fiabilite du rapport.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetMovementRepository movementRepository;
    @Mock
    private InventoryAnomalyRepository anomalyRepository;

    @InjectMocks
    private ReportService reportService;

    private static final ReportFilter NO_FILTER = new ReportFilter(null, null, null, null, null, null, null, null);

    @Test
    void parSite_groupsAssetsBySiteNameAndSumsAcquisitionValue() {
        Site siege = siteNamed("Siege");
        Site annexe = siteNamed("Annexe");
        when(assetRepository.findAll(any(Specification.class))).thenReturn(List.of(
                assetOf(siege, new BigDecimal("1000")),
                assetOf(siege, new BigDecimal("2000")),
                assetOf(annexe, new BigDecimal("500"))
        ));

        ReportResultDto result = reportService.generate(ReportType.PAR_SITE, NO_FILTER);

        assertThat(result.rows()).hasSize(2);
        // Trie alphabetiquement par libelle : Annexe avant Siege.
        assertThat(result.rows().get(0)).containsExactly("Annexe", "1", "500.00");
        assertThat(result.rows().get(1)).containsExactly("Siege", "2", "3000.00");
    }

    @Test
    void parUtilisateur_groupsByUserIdNeverByDisplayNameHomonyms() {
        // Regression : deux utilisateurs distincts peuvent porter le meme nom
        // affiche - regrouper par nom (bug corrige en Phase 9) les fusionnerait
        // a tort en une seule ligne. Doit toujours regrouper par UUID.
        Site site = siteNamed("Siege");
        User ahmedA = userNamed("Ahmed", "Amrani", site);
        User ahmedB = userNamed("Ahmed", "Amrani", site);

        Asset a1 = assetOf(site, BigDecimal.TEN);
        a1.setCurrentUser(ahmedA);
        Asset a2 = assetOf(site, BigDecimal.TEN);
        a2.setCurrentUser(ahmedB);

        when(assetRepository.findAll(any(Specification.class))).thenReturn(List.of(a1, a2));

        ReportResultDto result = reportService.generate(ReportType.PAR_UTILISATEUR, NO_FILTER);

        assertThat(result.rows()).hasSize(2);
    }

    @Test
    void reformes_fallsBackToDashWhenNoExecutedReformeMovementExists() {
        Site site = siteNamed("Siege");
        Asset seedReformed = assetOf(site, BigDecimal.ZERO);
        seedReformed.setStatus(AssetStatus.REFORME);
        seedReformed.setAssetCode("VECO-IMM-000012");

        Asset workflowReformed = assetOf(site, BigDecimal.ZERO);
        workflowReformed.setStatus(AssetStatus.REFORME);
        workflowReformed.setAssetCode("VECO-IMM-000099");

        AssetMovement executed = reformeMovement(workflowReformed, MovementStatus.EXECUTE, Instant.parse("2026-05-01T10:00:00Z"), "Panne irreparable");

        when(assetRepository.findAll(any(Specification.class))).thenReturn(List.of(seedReformed, workflowReformed));
        when(movementRepository.findAllByOrderByRequestedAtDesc()).thenReturn(List.of(executed));

        ReportResultDto result = reportService.generate(ReportType.REFORMES, NO_FILTER);

        assertThat(result.rows()).hasSize(2);
        List<String> seedRow = result.rows().stream().filter(r -> r.get(0).equals("VECO-IMM-000012")).findFirst().orElseThrow();
        assertThat(seedRow.get(4)).isEqualTo("-");
        assertThat(seedRow.get(5)).isEqualTo("-");

        List<String> workflowRow = result.rows().stream().filter(r -> r.get(0).equals("VECO-IMM-000099")).findFirst().orElseThrow();
        assertThat(workflowRow.get(4)).isNotEqualTo("-");
        assertThat(workflowRow.get(5)).isEqualTo("Panne irreparable");
    }

    @Test
    void reformes_picksMostRecentExecutedMovementWhenSeveralExist() {
        Site site = siteNamed("Siege");
        Asset asset = assetOf(site, BigDecimal.ZERO);
        asset.setStatus(AssetStatus.REFORME);
        asset.setAssetCode("VECO-IMM-000050");

        AssetMovement older = reformeMovement(asset, MovementStatus.EXECUTE, Instant.parse("2026-01-01T10:00:00Z"), "Ancien motif");
        AssetMovement newer = reformeMovement(asset, MovementStatus.EXECUTE, Instant.parse("2026-06-01T10:00:00Z"), "Motif recent");

        when(assetRepository.findAll(any(Specification.class))).thenReturn(List.of(asset));
        // findAllByOrderByRequestedAtDesc renvoie deja le plus recent en premier.
        when(movementRepository.findAllByOrderByRequestedAtDesc()).thenReturn(List.of(newer, older));

        ReportResultDto result = reportService.generate(ReportType.REFORMES, NO_FILTER);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).get(5)).isEqualTo("Motif recent");
    }

    @Test
    void anomalies_filtersByStatusSiteAndDateRange() {
        Site targetSite = siteNamed("Siege");
        Site otherSite = siteNamed("Annexe");

        InventoryAnomaly matching = anomalyOf(AnomalyStatus.NOUVELLE, targetSite, Instant.parse("2026-06-15T00:00:00Z"));
        InventoryAnomaly wrongStatus = anomalyOf(AnomalyStatus.RESOLUE, targetSite, Instant.parse("2026-06-15T00:00:00Z"));
        InventoryAnomaly wrongSite = anomalyOf(AnomalyStatus.NOUVELLE, otherSite, Instant.parse("2026-06-15T00:00:00Z"));
        InventoryAnomaly outOfRange = anomalyOf(AnomalyStatus.NOUVELLE, targetSite, Instant.parse("2020-01-01T00:00:00Z"));

        when(anomalyRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(matching, wrongStatus, wrongSite, outOfRange));

        ReportFilter filter = new ReportFilter(targetSite.getId(), null, null, null, null, AnomalyStatus.NOUVELLE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        ReportResultDto result = reportService.generate(ReportType.ANOMALIES, filter);

        assertThat(result.rows()).hasSize(1);
    }

    @Test
    void transferts_onlyReturnsInterSiteMovementsWhileMouvementsReturnsAll() {
        Site site = siteNamed("Siege");
        Asset asset = assetOf(site, BigDecimal.ZERO);
        AssetMovement transfert = movementOf(asset, MovementType.TRANSFERT_INTER_SITE);
        AssetMovement maintenance = movementOf(asset, MovementType.MAINTENANCE);

        when(movementRepository.findAllByOrderByRequestedAtDesc()).thenReturn(List.of(transfert, maintenance));

        ReportResultDto transferts = reportService.generate(ReportType.TRANSFERTS, NO_FILTER);
        assertThat(transferts.rows()).hasSize(1);

        ReportResultDto mouvements = reportService.generate(ReportType.MOUVEMENTS, NO_FILTER);
        assertThat(mouvements.rows()).hasSize(2);
    }

    // --- Helpers -------------------------------------------------

    private static Site siteNamed(String name) {
        Site site = new Site();
        site.setId(UUID.randomUUID());
        site.setName(name);
        return site;
    }

    private static User userNamed(String first, String last, Site site) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName(first);
        user.setLastName(last);
        user.setSite(site);
        return user;
    }

    private static Asset assetOf(Site site, BigDecimal value) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetCode("VECO-IMM-" + UUID.randomUUID().toString().substring(0, 6));
        asset.setDesignation("Materiel");
        asset.setSite(site);
        AssetCategory category = new AssetCategory();
        category.setId(UUID.randomUUID());
        category.setName("Informatique");
        asset.setCategory(category);
        asset.setAcquisitionValue(value);
        asset.setStatus(AssetStatus.EN_STOCK);
        asset.setDeleted(false);
        asset.setLabeled(true);
        return asset;
    }

    private static AssetMovement reformeMovement(Asset asset, MovementStatus status, Instant executedAt, String reason) {
        AssetMovement movement = movementOf(asset, MovementType.REFORME);
        movement.setStatus(status);
        movement.setExecutedAt(executedAt);
        movement.setReason(reason);
        return movement;
    }

    private static AssetMovement movementOf(Asset asset, MovementType type) {
        AssetMovement movement = new AssetMovement();
        movement.setId(UUID.randomUUID());
        movement.setAsset(asset);
        movement.setMovementType(type);
        movement.setStatus(MovementStatus.EXECUTE);
        movement.setRequestedAt(Instant.now());
        return movement;
    }

    private static InventoryAnomaly anomalyOf(AnomalyStatus status, Site site, Instant createdAt) {
        InventoryAnomaly anomaly = new InventoryAnomaly();
        anomaly.setId(UUID.randomUUID());
        InventoryCampaign campaign = new InventoryCampaign();
        campaign.setId(UUID.randomUUID());
        campaign.setName("Campagne");
        anomaly.setCampaign(campaign);
        Asset asset = assetOf(site, BigDecimal.ZERO);
        anomaly.setAsset(asset);
        anomaly.setAnomalyType(AnomalyType.AUTRE);
        anomaly.setStatus(status);
        anomaly.setCreatedAt(createdAt);
        return anomaly;
    }
}
