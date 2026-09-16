package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
    List<Floor> findByBuildingId(UUID buildingId);
    boolean existsByBuildingId(UUID buildingId);
    boolean existsByBuildingIdAndCodeIgnoreCaseAndIdNot(UUID buildingId, String code, UUID id);
    boolean existsByBuildingIdAndCodeIgnoreCase(UUID buildingId, String code);
}
