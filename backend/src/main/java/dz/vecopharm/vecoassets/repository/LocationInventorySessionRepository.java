package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.LocationInventorySession;
import dz.vecopharm.vecoassets.entity.LocationSessionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationInventorySessionRepository extends JpaRepository<LocationInventorySession, UUID> {

    // Garde-fou applicatif (message d'erreur clair) avant l'ouverture d'une
    // session - la garantie ultime reste l'index unique partiel en base
    // (V17, meme discipline que le compteur atomique par site en V15).
    boolean existsByLocationIdAndStatus(UUID locationId, LocationSessionStatus status);

    @EntityGraph(attributePaths = {"location", "openedBy", "validatedBy"})
    List<LocationInventorySession> findAllByOrderByOpenedAtDesc();

    @EntityGraph(attributePaths = {"location", "openedBy", "validatedBy"})
    List<LocationInventorySession> findByLocationIdOrderByOpenedAtDesc(UUID locationId);
}
