package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByZoneId(UUID zoneId);
    boolean existsByZoneId(UUID zoneId);
    boolean existsByZoneIdAndCodeIgnoreCaseAndIdNot(UUID zoneId, String code, UUID id);
    boolean existsByZoneIdAndCodeIgnoreCase(UUID zoneId, String code);

    // Checkpoint 1 "locaux scannables" (2026-09) : point d'entree du futur
    // scan de local (le QR imprime encode ce code) - GET /api/locations/by-code.
    Optional<Location> findByQrCode(String qrCode);

    // Comptage batche (pas de N+1) du nombre d'immobilisations non archivees
    // rattachees a chacun des locaux donnes, pour la colonne "Nb
    // immobilisations" de la liste des locaux - meme discipline que
    // Asset.listGraph/@EntityGraph cote AssetRepository. Chaque ligne du
    // resultat est [UUID locationId, Long count].
    @Query("select l.id, count(a) from Location l left join Asset a on a.location = l and a.deleted = false "
            + "where l.id in :ids group by l.id")
    List<Object[]> countAssetsByLocationIds(@Param("ids") List<UUID> ids);
}
