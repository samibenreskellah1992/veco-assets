package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AssetMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Phase 10 (optimisation N+1) : {@code MovementMapper.toDto} derefence
 * jusqu'a 9 associations *-to-one par mouvement (immobilisation, sites et
 * locaux origine/destination, utilisateurs origine/destination,
 * demandeur/validateur) - toutes {@code FetchType.LAZY}. {@code /mouvements}
 * (Phase 8) et les rapports MOUVEMENTS/TRANSFERTS/REFORMES (Phase 9) passent
 * tous par les deux methodes ci-dessous : l'entity graph evite qu'une liste
 * de N mouvements ne declenche jusqu'a 9*N requetes supplementaires.
 */
public interface AssetMovementRepository extends JpaRepository<AssetMovement, UUID> {

    @EntityGraph(attributePaths = {"asset", "asset.site", "fromSite", "toSite", "fromLocation", "toLocation", "fromUser", "toUser", "requestedBy", "validatedBy"})
    List<AssetMovement> findByAssetIdOrderByRequestedAtDesc(UUID assetId);

    @EntityGraph(attributePaths = {"asset", "asset.site", "fromSite", "toSite", "fromLocation", "toLocation", "fromUser", "toUser", "requestedBy", "validatedBy"})
    List<AssetMovement> findAllByOrderByRequestedAtDesc();
}
