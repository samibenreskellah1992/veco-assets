package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.InventoryScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryScanRepository extends JpaRepository<InventoryScan, UUID> {
    List<InventoryScan> findByCampaignId(UUID campaignId);
    List<InventoryScan> findByCampaignIdAndAssetId(UUID campaignId, UUID assetId);
}
