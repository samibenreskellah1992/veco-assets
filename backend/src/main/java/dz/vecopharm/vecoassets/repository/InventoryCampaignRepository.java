package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryCampaignRepository extends JpaRepository<InventoryCampaign, UUID> {
    List<InventoryCampaign> findBySiteId(UUID siteId);

    // Phase 7 : liste complete triee, filtree en memoire par InventoryCampaignService
    // (site/statut) - le nombre de campagnes reste modeste (une poignee par
    // an et par site), meme convention que le referentiel (Phase 4).
    // Phase 10 : entity graph quand meme - InventoryCampaignMapper.toDto
    // derefence site/zone/responsibleUser par ligne, et rien n'empeche ce
    // nombre "modeste" de grandir avec le temps.
    @EntityGraph(attributePaths = {"site", "zone", "responsibleUser"})
    List<InventoryCampaign> findAllByOrderByStartDateDesc();
}
