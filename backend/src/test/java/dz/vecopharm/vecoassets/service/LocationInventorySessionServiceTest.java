package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.LocationSessionProgressDto;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.LocationInventorySession;
import dz.vecopharm.vecoassets.entity.LocationSessionStatus;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AssetMapper;
import dz.vecopharm.vecoassets.mapper.LocationInventorySessionMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import dz.vecopharm.vecoassets.repository.InventoryScanRepository;
import dz.vecopharm.vecoassets.repository.LocationInventorySessionRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito) de {@link LocationInventorySessionService} -
 * Checkpoint 3 de l'evolution "locaux scannables" (2026-09), ajoutes apres
 * la verification live du checkpoint (voir doc de suivi du projet).
 *
 * <p>Couvre les trois regles metier qui n'etaient jusqu'ici verifiees
 * qu'en conditions reelles dans le navigateur : (1) une seule session
 * {@code EN_COURS} par local a la fois (le controle applicatif ici,
 * complementaire de l'index unique partiel V17 verifie cote base par
 * {@link LocationInventorySessionControllerIntegrationTest}), (2) un
 * local desactive ne peut pas recevoir de nouvelle session, et (3) la
 * validation fige la session ET met a jour {@code Location.lastInventoryAt}
 * - jusque-la jamais ecrit avant ce checkpoint. Le calcul de progression
 * (memes regles que {@code InventoryCampaignService#progress}) est
 * verifie separement.</p>
 */
@ExtendWith(MockitoExtension.class)
class LocationInventorySessionServiceTest {

    @Mock
    private LocationInventorySessionRepository sessionRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryScanRepository scanRepository;
    @Mock
    private InventoryAnomalyRepository anomalyRepository;
    @Mock
    private LocationInventorySessionMapper sessionMapper;
    @Mock
    private AssetMapper assetMapper;
    @Mock
    private dz.vecopharm.vecoassets.audit.AuditRecorder auditRecorder;

    @InjectMocks
    private LocationInventorySessionService service;

    private UUID locationId;
    private Location location;

    @BeforeEach
    void setUp() {
        locationId = UUID.randomUUID();
        location = new Location();
        location.setId(locationId);
        location.setActive(true);

        lenient().when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void open_rejectsWhenLocationInactive() {
        location.setActive(false);
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.open(locationId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("desactive");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void open_rejectsWhenLocationDoesNotExist() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(locationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void open_rejectsWhenASessionIsAlreadyEnCoursForThisLocation() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(sessionRepository.existsByLocationIdAndStatus(locationId, LocationSessionStatus.EN_COURS)).thenReturn(true);

        assertThatThrownBy(() -> service.open(locationId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deja en cours");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void open_success_createsAnEnCoursSessionAndAudits() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(sessionRepository.existsByLocationIdAndStatus(locationId, LocationSessionStatus.EN_COURS)).thenReturn(false);

        service.open(locationId);

        ArgumentCaptor<LocationInventorySession> captor = ArgumentCaptor.forClass(LocationInventorySession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getLocation()).isEqualTo(location);
        assertThat(captor.getValue().getStatus()).isEqualTo(LocationSessionStatus.EN_COURS);
        assertThat(captor.getValue().getOpenedAt()).isNotNull();
        verify(auditRecorder, org.mockito.Mockito.times(1)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void validate_rejectsWhenSessionIsNotEnCours() {
        LocationInventorySession session = sessionOn(location, LocationSessionStatus.VALIDEE);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.validate(session.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("VALIDEE");

        verify(locationRepository, never()).save(any());
    }

    @Test
    void validate_rejectsWhenSessionDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validate_success_freezesTheSessionAndStampsLocationLastInventoryAt() {
        LocationInventorySession session = sessionOn(location, LocationSessionStatus.EN_COURS);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        lenient().when(locationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.validate(session.getId());

        assertThat(session.getStatus()).isEqualTo(LocationSessionStatus.VALIDEE);
        assertThat(session.getValidatedAt()).isNotNull();

        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository).save(locationCaptor.capture());
        assertThat(locationCaptor.getValue().getLastInventoryAt()).isEqualTo(session.getValidatedAt());
        verify(auditRecorder, org.mockito.Mockito.times(1)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void progress_countsOnlyScansAndAnomaliesInsideTheLocationScope() {
        LocationInventorySession session = sessionOn(location, LocationSessionStatus.EN_COURS);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        UUID inScopeAsset1 = UUID.randomUUID();
        UUID inScopeAsset2 = UUID.randomUUID();
        UUID inScopeAsset3 = UUID.randomUUID();
        when(assetRepository.findByLocationIdAndDeletedFalse(locationId))
                .thenReturn(List.of(assetWithId(inScopeAsset1), assetWithId(inScopeAsset2), assetWithId(inScopeAsset3)));

        // Un des deux scans (l'immobilisation "hors local") n'est PAS dans le perimetre attendu :
        // c'est une immobilisation d'un autre local, donc son id n'appartient pas a scopeAssetIds.
        UUID outOfScopeAsset = UUID.randomUUID();
        when(scanRepository.findDistinctAssetIdsByLocationSessionId(session.getId()))
                .thenReturn(Set.of(inScopeAsset1, outOfScopeAsset));
        when(scanRepository.findDistinctPresentAssetIdsByLocationSessionId(session.getId()))
                .thenReturn(Set.of(inScopeAsset1));
        when(anomalyRepository.countByLocationSessionId(session.getId())).thenReturn(1L);

        LocationSessionProgressDto progress = service.progress(session.getId());

        assertThat(progress.totalAssetsExpected()).isEqualTo(3);
        // Seul inScopeAsset1 compte : outOfScopeAsset est ignore malgre son scan.
        assertThat(progress.scannedAssetsCount()).isEqualTo(1);
        assertThat(progress.presentCount()).isEqualTo(1);
        assertThat(progress.anomaliesCount()).isEqualTo(1);
        assertThat(progress.remainingCount()).isEqualTo(2);
        assertThat(progress.progressPercent()).isEqualTo(33.33);
    }

    @Test
    void progress_withNoAssetsExpected_isZeroPercentNotDivisionByZero() {
        LocationInventorySession session = sessionOn(location, LocationSessionStatus.EN_COURS);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(assetRepository.findByLocationIdAndDeletedFalse(locationId)).thenReturn(List.of());
        when(scanRepository.findDistinctAssetIdsByLocationSessionId(session.getId())).thenReturn(Set.of());
        when(scanRepository.findDistinctPresentAssetIdsByLocationSessionId(session.getId())).thenReturn(Set.of());
        when(anomalyRepository.countByLocationSessionId(session.getId())).thenReturn(0L);

        LocationSessionProgressDto progress = service.progress(session.getId());

        assertThat(progress.totalAssetsExpected()).isZero();
        assertThat(progress.progressPercent()).isZero();
        assertThat(progress.remainingCount()).isZero();
    }

    private LocationInventorySession sessionOn(Location loc, LocationSessionStatus status) {
        LocationInventorySession session = new LocationInventorySession();
        session.setId(UUID.randomUUID());
        session.setLocation(loc);
        session.setStatus(status);
        session.setOpenedAt(java.time.Instant.now());
        return session;
    }

    private dz.vecopharm.vecoassets.entity.Asset assetWithId(UUID id) {
        dz.vecopharm.vecoassets.entity.Asset asset = new dz.vecopharm.vecoassets.entity.Asset();
        asset.setId(id);
        return asset;
    }
}
