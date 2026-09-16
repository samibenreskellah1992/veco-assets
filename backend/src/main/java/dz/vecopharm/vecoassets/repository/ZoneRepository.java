package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {
    List<Zone> findByFloorId(UUID floorId);
}
