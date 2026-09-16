package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * {@link JpaSpecificationExecutor} is included from Phase 2 already so the
 * Phase 5 (Immobilisations) filtre combinable - site + categorie + etat +
 * statut + etiquete... - can be built on {@code specification/} without
 * changing this contract later.
 */
public interface AssetRepository extends JpaRepository<Asset, UUID>, JpaSpecificationExecutor<Asset> {
    Optional<Asset> findByAssetCode(String assetCode);
    Optional<Asset> findBySerialNumber(String serialNumber);

    // Utilisees par le Phase 4 referentiel (SiteService, BuildingService, ...)
    // pour bloquer la suppression d'une donnee encore utilisee par au moins
    // une immobilisation - en plus de la contrainte FK en base (defense en
    // profondeur, message d'erreur clair cote API plutot qu'une erreur SQL brute).
    boolean existsBySiteId(UUID siteId);
    boolean existsByBuildingId(UUID buildingId);
    boolean existsByFloorId(UUID floorId);
    boolean existsByZoneId(UUID zoneId);
    boolean existsByLocationId(UUID locationId);
    boolean existsByCategoryId(UUID categoryId);
}
