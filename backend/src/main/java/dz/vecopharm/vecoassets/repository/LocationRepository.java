package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByZoneId(UUID zoneId);
}
