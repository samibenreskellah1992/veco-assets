package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryCampaignRepository extends JpaRepository<InventoryCampaign, UUID> {
    List<InventoryCampaign> findBySiteId(UUID siteId);
}
