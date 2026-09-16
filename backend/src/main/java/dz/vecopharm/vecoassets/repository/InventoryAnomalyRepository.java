package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryAnomalyRepository extends JpaRepository<InventoryAnomaly, UUID> {
    List<InventoryAnomaly> findByCampaignId(UUID campaignId);
}
