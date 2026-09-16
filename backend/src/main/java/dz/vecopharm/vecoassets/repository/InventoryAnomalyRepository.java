package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Phase 10 (optimisation N+1) : {@code InventoryAnomalyMapper.toDto} et le
 * rapport ANOMALIES (Phase 9) derefencent campagne/immobilisation/site de
 * l'immobilisation/signaleur par ligne - entity graph sur les deux methodes
 * de liste pour eviter une requete par association et par ligne. {@code
 * countByCampaignId} reste une requete d'agregation, aucune association
 * n'est chargee.
 */
public interface InventoryAnomalyRepository extends JpaRepository<InventoryAnomaly, UUID> {

    @EntityGraph(attributePaths = {"campaign", "asset", "asset.site", "reportedBy"})
    List<InventoryAnomaly> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);

    @EntityGraph(attributePaths = {"campaign", "asset", "asset.site", "reportedBy"})
    List<InventoryAnomaly> findAllByOrderByCreatedAtDesc();

    long countByCampaignId(UUID campaignId);
}
