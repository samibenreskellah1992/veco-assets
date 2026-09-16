package dz.vecopharm.vecoassets.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tableau de bord (prompt maitre Phase 9) : chaque champ est recalcule a la
 * demande depuis les tables reelles (assets / asset_movements /
 * inventory_anomalies / audit_logs), jamais un compteur stocke - meme
 * discipline que {@link CampaignProgressDto} en Phase 7 ("chaque chiffre du
 * dashboard est verifiable contre la base de donnees").
 */
public record DashboardDto(
        long totalAssets,
        long labeledAssets,
        long unlabeledAssets,
        long inventoriedAssets,
        long notInventoriedAssets,
        long openAnomalies,
        long inStock,
        long inService,
        long inMaintenance,
        long reformed,
        long exited,
        BigDecimal totalAcquisitionValue,
        List<CountByLabelDto> bySite,
        List<CountByLabelDto> byCategory,
        List<CountByLabelDto> byCondition,
        List<AuditLogDto> recentActivity
) {
}
