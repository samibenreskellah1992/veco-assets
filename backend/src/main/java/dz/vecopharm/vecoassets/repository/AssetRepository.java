package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link JpaSpecificationExecutor} is included from Phase 2 already so the
 * Phase 5 (Immobilisations) filtre combinable - site + categorie + etat +
 * statut + etiquete... - can be built on {@code specification/} without
 * changing this contract later.
 *
 * <p>Phase 10 (optimisation N+1) : {@code AssetMapper}/{@code AssetDto}
 * derefence systematiquement categorie, toute la hierarchie de localisation
 * et les deux utilisateurs (courant/responsable) pour chaque immobilisation
 * affichee - et les rapports/dashboard (Phase 9) font de meme pour
 * site/categorie/utilisateur courant. Sans entity graph, une page de liste
 * de N immobilisations declenchait jusqu'a 8*N requetes SQL supplementaires
 * (une par association *-to-one par ligne, toutes en {@code FetchType.LAZY}
 * - voir {@link Asset}). Les methodes ci-dessous qui alimentent un ecran de
 * liste ou un rapport sont donc annotees {@link EntityGraph}, reprenant le
 * graphe nomme {@code Asset.listGraph} declare sur l'entite, pour charger
 * ces associations en un seul SELECT (JOIN FETCH) au lieu d'une requete par
 * association et par ligne.</p>
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

    // Phase 7 : perimetre attendu d'une campagne d'inventaire (site, et zone
    // si la campagne en cible une) - toujours restreint aux immobilisations
    // non archivees, une immobilisation reformee/archivee ne fait plus
    // partie du parc a inventorier. InventoryCampaignService en derive a la
    // fois la taille du perimetre et les ids (pour le croiser avec les
    // scans), d'ou des methodes retournant la liste plutot qu'un simple COUNT.
    @EntityGraph(value = "Asset.listGraph")
    List<Asset> findBySiteIdAndDeletedFalse(UUID siteId);

    @EntityGraph(value = "Asset.listGraph")
    List<Asset> findBySiteIdAndZoneIdAndDeletedFalse(UUID siteId, UUID zoneId);

    // Checkpoint 3 "locaux scannables" (2026-09) : perimetre attendu d'une
    // session de scan de local (LocationInventorySessionService) - meme
    // discipline que les deux methodes ci-dessus pour les campagnes.
    @EntityGraph(value = "Asset.listGraph")
    List<Asset> findByLocationIdAndDeletedFalse(UUID locationId);

    // Phase 9 (Reporting) : base commune du tableau de bord et des rapports
    // "par site/categorie/etat/service/utilisateur/non etiquetees/non
    // inventoriees" - toujours restreinte au parc actif (non archive), les
    // regroupements/filtres complementaires se font en Java (DashboardService,
    // ReportService), meme discipline que InventoryCampaignService.progress()
    // en Phase 7 plutot qu'une requete d'agregation SQL par indicateur.
    @EntityGraph(value = "Asset.listGraph")
    List<Asset> findByDeletedFalse();

    // Phase 10 : redeclaration des methodes JpaSpecificationExecutor utilisees
    // par AssetService.list() (ecran /immobilisations, le plus consulte de
    // l'application) et ReportService.filteredAssets() (Phase 9), pour leur
    // attacher l'entity graph ci-dessus - sinon @EntityGraph ne peut pas etre
    // pose sur une methode heritee sans la redeclarer dans cette interface.
    @Override
    @EntityGraph(value = "Asset.listGraph")
    List<Asset> findAll(Specification<Asset> spec);

    @Override
    @EntityGraph(value = "Asset.listGraph")
    Page<Asset> findAll(Specification<Asset> spec, Pageable pageable);
}
