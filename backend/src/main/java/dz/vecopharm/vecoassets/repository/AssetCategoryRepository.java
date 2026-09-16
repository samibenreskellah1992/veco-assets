package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, UUID> {
    Optional<AssetCategory> findByCode(String code);
    List<AssetCategory> findByParentId(UUID parentId);
    boolean existsByParentId(UUID parentId);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
