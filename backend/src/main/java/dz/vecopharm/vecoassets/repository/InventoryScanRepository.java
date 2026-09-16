package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.InventoryScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InventoryScanRepository extends JpaRepository<InventoryScan, UUID> {
    List<InventoryScan> findByCampaignIdOrderByScannedAtDesc(UUID campaignId);

    // Alimente l'onglet "Inventaire" de la fiche immobilisation (Phase 5,
    // jusqu'ici un placeholder honnete faute de module Phase 7) - tout
    // l'historique de scan d'un bien, toutes campagnes confondues.
    List<InventoryScan> findByAssetIdOrderByScannedAtDesc(UUID assetId);

    // Renvoient des ensembles d'ids (pas de simples COUNT) car la progression
    // d'une campagne (InventoryCampaignService#progress) doit croiser ces
    // scans avec le perimetre reel (site/zone) de la campagne : un scan sur
    // une immobilisation hors perimetre (anomalie MAUVAISE_LOCALISATION) ne
    // doit jamais compter comme "immobilisation attendue scannee", sous
    // peine de fausser le compteur restant - bug trouve et corrige pendant
    // la verification SQL de cette phase (voir docs/ROADMAP.md section 13).
    @Query("select distinct s.asset.id from InventoryScan s where s.campaign.id = :campaignId")
    Set<UUID> findDistinctAssetIdsByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("select distinct s.asset.id from InventoryScan s where s.campaign.id = :campaignId and s.result = dz.vecopharm.vecoassets.entity.ScanResult.PRESENT")
    Set<UUID> findDistinctPresentAssetIdsByCampaignId(@Param("campaignId") UUID campaignId);
}
