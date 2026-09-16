package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryCampaignRepository extends JpaRepository<InventoryCampaign, UUID> {
    List<InventoryCampaign> findBySiteId(UUID siteId);

    // Phase 7 : liste complete triee, filtree en memoire par InventoryCampaignService
    // (site/statut) - le nombre de campagnes reste modeste (une poignee par
    // an et par site), meme convention que le referentiel (Phase 4).
    List<InventoryCampaign> findAllByOrderByStartDateDesc();
}
