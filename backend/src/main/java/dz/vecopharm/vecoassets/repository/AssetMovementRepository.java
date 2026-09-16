package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AssetMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetMovementRepository extends JpaRepository<AssetMovement, UUID> {
    List<AssetMovement> findByAssetIdOrderByRequestedAtDesc(UUID assetId);
}
