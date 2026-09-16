package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building, UUID> {
    List<Building> findBySiteId(UUID siteId);
}
