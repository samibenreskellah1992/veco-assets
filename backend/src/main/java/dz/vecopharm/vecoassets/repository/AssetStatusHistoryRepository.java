package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AssetStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetStatusHistoryRepository extends JpaRepository<AssetStatusHistory, UUID> {
    List<AssetStatusHistory> findByAssetIdOrderByChangedAtDesc(UUID assetId);
}
