package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AssetLabelFormat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetLabelFormatRepository extends JpaRepository<AssetLabelFormat, UUID> {
    Optional<AssetLabelFormat> findByCode(String code);
    List<AssetLabelFormat> findByActiveTrue();
}
