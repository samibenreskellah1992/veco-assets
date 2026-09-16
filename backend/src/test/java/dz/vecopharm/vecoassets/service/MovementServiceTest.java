package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.MovementCreateRequest;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetMovement;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.Building;
import dz.vecopharm.vecoassets.entity.Floor;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.MovementStatus;
import dz.vecopharm.vecoassets.entity.MovementType;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.entity.Zone;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.mapper.AssetMapper;
import dz.vecopharm.vecoassets.mapper.MovementMapper;
import dz.vecopharm.vecoassets.repository.AssetAssignmentRepository;
import dz.vecopharm.vecoassets.repository.AssetMovementRepository;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.AssetStatusHistoryRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito, aucun contexte Spring / base de donnees) du
 * workflow Demande -> Validation -> Execution (prompt maitre Phase 8 /
 * section 18) - le module le plus a risque de l'application au sens ou il
 * modifie {@link Asset} pour les 9 {@link MovementType}. Couvre : le blocage
 * d'une immobilisation reformee/archivee des la demande, la validation des
 * champs cibles par type de mouvement (applyTargetFields), et l'effet reel
 * de chaque type a l'execution (applyEffects) - jamais avant, conformement
 * au workflow.
 */
@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private AssetMovementRepository movementRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetAssignmentRepository assignmentRepository;
    @Mock
    private AssetStatusHistoryRepository statusHistoryRepository;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MovementMapper movementMapper;
    @Mock
    private AssetMapper assetMapper;
    @Mock
    private AuditRecorder auditRecorder;

    @InjectMocks
    private MovementService movementService;

    private Site site;
    private Asset asset;

    @BeforeEach
    void setUp() {
        site = new Site();
        site.setId(UUID.randomUUID());
        site.setName("Siege");

        asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetCode("VECO-IMM-000001");
        asset.setSite(site);
        asset.setStatus(AssetStatus.EN_STOCK);
        asset.setCondition(AssetCondition.BON);
        asset.setDeleted(false);

        // Echo l'entite recue plutot que de renvoyer null (comportement par
        // defaut de Mockito), pour que les assertions sur l'objet mute
        // restent valables apres l'appel du service.
        lenient().when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(assignmentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private MovementCreateRequest requestFor(MovementType type, UUID toSiteId, UUID toLocationId, UUID toUserId,
                                               String toDirection, String toDepartment, String toService) {
        return new MovementCreateRequest(asset.getId(), type, toSiteId, toLocationId, toUserId,
                toDirection, toDepartment, toService, "motif", "commentaire");
    }

    // --- Blocage sur immobilisation archivee/reformee -------------------------------------------------

    @Test
    void request_rejectsArchivedAsset() {
        asset.setDeleted(true);
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> movementService.request(requestFor(MovementType.MAINTENANCE, null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("archivee");
    }

    @Test
    void request_rejectsReformedAsset() {
        asset.setStatus(AssetStatus.REFORME);
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> movementService.request(requestFor(MovementType.RETOUR, null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reformee");
    }

    // --- Demande : validation des champs cibles par type -------------------------------------------------

    @Test
    void request_affectation_requiresToUser() {
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> movementService.request(requestFor(MovementType.AFFECTATION, null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("utilisateur destinataire");
    }

    @Test
    void request_affectation_capturesFromFieldsAndResolvesTargetUser() {
        User toUser = new User();
        toUser.setId(UUID.randomUUID());
        toUser.setFirstName("Amine");
        toUser.setLastName("Benali");

        asset.setDirection("Direction IT");
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(userRepository.findById(toUser.getId())).thenReturn(Optional.of(toUser));

        movementService.request(requestFor(MovementType.AFFECTATION, null, null, toUser.getId(), "Direction IT", "Dept", "Service"));

        var captor = org.mockito.ArgumentCaptor.forClass(AssetMovement.class);
        org.mockito.Mockito.verify(movementRepository).save(captor.capture());
        AssetMovement saved = captor.getValue();
        assertThat(saved.getFromSite()).isEqualTo(site);
        assertThat(saved.getFromDirection()).isEqualTo("Direction IT");
        assertThat(saved.getToUser()).isEqualTo(toUser);
        assertThat(saved.getStatus()).isEqualTo(MovementStatus.DEMANDE);
    }

    @Test
    void request_transfertInterSite_requiresToSite() {
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> movementService.request(requestFor(MovementType.TRANSFERT_INTER_SITE, null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("site de destination");
    }

    @Test
    void request_transfertInterSite_rejectsSameSiteAsDestination() {
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));

        assertThatThrownBy(() -> movementService.request(requestFor(MovementType.TRANSFERT_INTER_SITE, site.getId(), null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different");
    }

    @Test
    void request_changementLocalisation_rejectsLocationOutsideCurrentSite() {
        Site otherSite = new Site();
        otherSite.setId(UUID.randomUUID());
        Location location = locationIn(otherSite);

        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> movementService.request(requestFor(MovementType.CHANGEMENT_LOCALISATION, null, location.getId(), null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("site actuel");
    }

    @Test
    void request_changementService_requiresAtLeastOneTargetField() {
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> movementService.request(requestFor(MovementType.CHANGEMENT_SERVICE, null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("service");
    }

    // --- Execution : effet reel sur l'immobilisation, uniquement depuis VALIDE -------------------------------------------------

    @Test
    void execute_rejectsMovementNotValide() {
        AssetMovement movement = movementOf(MovementType.MAINTENANCE, MovementStatus.DEMANDE);
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        assertThatThrownBy(() -> movementService.execute(movement.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("executee");
    }

    @Test
    void execute_rejectsWhenAssetArchivedSinceValidation() {
        asset.setDeleted(true);
        AssetMovement movement = movementOf(MovementType.MAINTENANCE, MovementStatus.VALIDE);
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        assertThatThrownBy(() -> movementService.execute(movement.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("archivee");
    }

    @Test
    void execute_affectation_setsCurrentUserAndPromotesToEnService() {
        User toUser = new User();
        toUser.setId(UUID.randomUUID());
        AssetMovement movement = movementOf(MovementType.AFFECTATION, MovementStatus.VALIDE);
        movement.setToUser(toUser);
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        movementService.execute(movement.getId());

        assertThat(asset.getCurrentUser()).isEqualTo(toUser);
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.EN_SERVICE);
        assertThat(movement.getStatus()).isEqualTo(MovementStatus.EXECUTE);
        assertThat(movement.getExecutedAt()).isNotNull();
    }

    @Test
    void execute_retour_clearsCurrentUserAndSetsEnStock() {
        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        asset.setCurrentUser(currentUser);
        asset.setStatus(AssetStatus.EN_SERVICE);
        AssetMovement movement = movementOf(MovementType.RETOUR, MovementStatus.VALIDE);
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        movementService.execute(movement.getId());

        assertThat(asset.getCurrentUser()).isNull();
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.EN_STOCK);
    }

    @Test
    void execute_maintenance_setsEnMaintenance() {
        AssetMovement movement = movementOf(MovementType.MAINTENANCE, MovementStatus.VALIDE);
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        movementService.execute(movement.getId());

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.EN_MAINTENANCE);
    }

    @Test
    void execute_sortie_setsSorti() {
        AssetMovement movement = movementOf(MovementType.SORTIE, MovementStatus.VALIDE);
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        movementService.execute(movement.getId());

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.SORTI);
    }

    @Test
    void execute_reforme_setsStatusAndConditionReforme() {
        AssetMovement movement = movementOf(MovementType.REFORME, MovementStatus.VALIDE);
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        movementService.execute(movement.getId());

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.REFORME);
        assertThat(asset.getCondition()).isEqualTo(AssetCondition.REFORME);
    }

    @Test
    void execute_transfertInterSite_changesSiteAndClearsLocationWhenNoTargetLocationGiven() {
        Site destination = new Site();
        destination.setId(UUID.randomUUID());
        destination.setName("Annexe");
        asset.setBuilding(new Building());
        asset.setFloor(new Floor());
        asset.setZone(new Zone());
        asset.setLocation(locationIn(site));

        AssetMovement movement = movementOf(MovementType.TRANSFERT_INTER_SITE, MovementStatus.VALIDE);
        movement.setToSite(destination);
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        movementService.execute(movement.getId());

        assertThat(asset.getSite()).isEqualTo(destination);
        assertThat(asset.getBuilding()).isNull();
        assertThat(asset.getFloor()).isNull();
        assertThat(asset.getZone()).isNull();
        assertThat(asset.getLocation()).isNull();
    }

    @Test
    void execute_changementService_updatesFieldsWithoutTouchingStatus() {
        asset.setDirection("Ancienne direction");
        AssetMovement movement = movementOf(MovementType.CHANGEMENT_SERVICE, MovementStatus.VALIDE);
        movement.setToDirection("Nouvelle direction");
        movement.setToService("Nouveau service");
        when(movementRepository.findById(movement.getId())).thenReturn(Optional.of(movement));

        movementService.execute(movement.getId());

        assertThat(asset.getDirection()).isEqualTo("Nouvelle direction");
        assertThat(asset.getService()).isEqualTo("Nouveau service");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.EN_STOCK);
    }

    // --- Helpers -------------------------------------------------

    private AssetMovement movementOf(MovementType type, MovementStatus status) {
        AssetMovement movement = new AssetMovement();
        movement.setId(UUID.randomUUID());
        movement.setAsset(asset);
        movement.setMovementType(type);
        movement.setStatus(status);
        movement.setRequestedAt(java.time.Instant.now());
        if (status == MovementStatus.VALIDE) {
            movement.setValidatedAt(java.time.Instant.now());
        }
        return movement;
    }

    private Location locationIn(Site targetSite) {
        Building building = new Building();
        building.setSite(targetSite);
        Floor floor = new Floor();
        floor.setBuilding(building);
        Zone zone = new Zone();
        zone.setFloor(floor);
        Location location = new Location();
        location.setId(UUID.randomUUID());
        location.setZone(zone);
        return location;
    }
}
