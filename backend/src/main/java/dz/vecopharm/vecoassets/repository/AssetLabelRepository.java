package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AssetLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetLabelRepository extends JpaRepository<AssetLabel, UUID> {
    List<AssetLabel> findByAssetId(UUID assetId);
}
