package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.LocationLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Checkpoint 2 "locaux scannables" (2026-09) - mirroir de {@link AssetLabelRepository}. */
public interface LocationLabelRepository extends JpaRepository<LocationLabel, UUID> {
    List<LocationLabel> findByLocationIdOrderByGeneratedAtDesc(UUID locationId);

    // Utilisee par AssetLabelFormatService pour bloquer la suppression d'un
    // format encore utilise par au moins une etiquette de local generee
    // (le format est partage entre immobilisations et locaux - V16).
    boolean existsByFormatId(UUID formatId);
}
