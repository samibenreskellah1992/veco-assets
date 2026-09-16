package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, UUID> {
    List<AssetAssignment> findByAssetIdOrderByAssignedFromDesc(UUID assetId);
    Optional<AssetAssignment> findByAssetIdAndAssignedUntilIsNull(UUID assetId);
}
