package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.AuditLogDto;
import dz.vecopharm.vecoassets.dto.CountByLabelDto;
import dz.vecopharm.vecoassets.dto.DashboardDto;
import dz.vecopharm.vecoassets.entity.AnomalyStatus;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tableau de bord (prompt maitre Phase 9). Chaque indicateur est recalcule
 * a la demande a partir du parc actif reel ({@link AssetRepository#findByDeletedFalse()})
 * - jamais un compteur stocke qui pourrait diverger - exactement la meme
 * discipline que {@code InventoryCampaignService.progress()} en Phase 7.
 * "Activite recente" reutilise directement {@link AuditLogService#recent(int)}
 * (deja reserve a ADMIN_ACCESS sur son propre endpoint) : l'appeler ici en
 * service-a-service, derriere {@code DashboardController} qui exige
 * REPORT_VIEW, expose volontairement un extrait limite (10 evenements) sans
 * dupliquer sa logique de lecture.
 */
@Service
public class DashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final AssetRepository assetRepository;
    private final InventoryAnomalyRepository anomalyRepository;
    private final AuditLogService auditLogService;

    public DashboardService(AssetRepository assetRepository, InventoryAnomalyRepository anomalyRepository, AuditLogService auditLogService) {
        this.assetRepository = assetRepository;
        this.anomalyRepository = anomalyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public DashboardDto get() {
        List<Asset> assets = assetRepository.findByDeletedFalse();

        long total = assets.size();
        long labeled = assets.stream().filter(Asset::isLabeled).count();
        long inventoried = assets.stream().filter(a -> a.getLastInventoryAt() != null).count();
        long inStock = countByStatus(assets, AssetStatus.EN_STOCK);
        long inService = countByStatus(assets, AssetStatus.EN_SERVICE);
        long inMaintenance = countByStatus(assets, AssetStatus.EN_MAINTENANCE);
        long reformed = countByStatus(assets, AssetStatus.REFORME);
        long exited = countByStatus(assets, AssetStatus.SORTI);

        long openAnomalies = anomalyRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> a.getStatus() == AnomalyStatus.NOUVELLE || a.getStatus() == AnomalyStatus.EN_COURS)
                .count();

        BigDecimal totalValue = assets.stream()
                .map(Asset::getAcquisitionValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AuditLogDto> recentActivity = auditLogService.recent(RECENT_ACTIVITY_LIMIT);

        return new DashboardDto(
                total,
                labeled,
                total - labeled,
                inventoried,
                total - inventoried,
                openAnomalies,
                inStock,
                inService,
                inMaintenance,
                reformed,
                exited,
                totalValue,
                groupBy(assets, a -> a.getSite().getName()),
                groupBy(assets, a -> a.getCategory().getName()),
                groupBy(assets, a -> a.getCondition().name()),
                recentActivity
        );
    }

    private static long countByStatus(List<Asset> assets, AssetStatus status) {
        return assets.stream().filter(a -> a.getStatus() == status).count();
    }

    /** Regroupe et trie par nombre decroissant (puis alphabetique) - ordre le plus lisible pour un graphique en barres. */
    private static List<CountByLabelDto> groupBy(List<Asset> assets, java.util.function.Function<Asset, String> classifier) {
        Map<String, Long> counts = assets.stream()
                .collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> new CountByLabelDto(e.getKey(), e.getValue()))
                .sorted(Comparator.<CountByLabelDto>comparingLong(CountByLabelDto::count).reversed()
                        .thenComparing(CountByLabelDto::label))
                .toList();
    }
}
