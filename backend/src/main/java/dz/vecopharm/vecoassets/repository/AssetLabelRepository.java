package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AssetLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetLabelRepository extends JpaRepository<AssetLabel, UUID> {
    List<AssetLabel> findByAssetIdOrderByGeneratedAtDesc(UUID assetId);

    // Utilisee par AssetLabelFormatService pour bloquer la suppression d'un
    // format encore utilise par au moins une etiquette generee (meme
    // convention de defense en profondeur qu'en Phase 4 - voir
    // AssetRepository.existsBySiteId et consorts).
    boolean existsByFormatId(UUID formatId);
}
