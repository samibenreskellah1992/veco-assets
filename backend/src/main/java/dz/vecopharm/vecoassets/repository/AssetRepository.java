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
}
