package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.LocationLabelDto;
import dz.vecopharm.vecoassets.entity.AssetLabelFormat;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.LocationLabel;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.LocationLabelMapper;
import dz.vecopharm.vecoassets.repository.AssetLabelFormatRepository;
import dz.vecopharm.vecoassets.repository.LocationLabelRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito, aucun contexte Spring / base de donnees) de
 * {@link LocationLabelService} (Checkpoint 2 "locaux scannables", 2026-09) -
 * meme structure que {@link MovementServiceTest}. La generation reelle du
 * contenu PDF/QR est deja verifiee sans mock par
 * {@link LabelGenerationContentTest} (directement via LabelPdfBuilder) ; ce
 * qui est propre a ce service-ci - format introuvable/desactive, local
 * introuvable/desactive (et le message doit nommer le local fautif), et
 * surtout chaque local marque "etiquete" + son historique trace + l'audit
 * enregistre, jamais silencieusement - est couvert ici.
 */
@ExtendWith(MockitoExtension.class)
class LocationLabelServiceTest {

    @Mock
    private LocationRepository locationRepository;
    @Mock
    private AssetLabelFormatRepository formatRepository;
    @Mock
    private LocationLabelRepository locationLabelRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LocationLabelMapper locationLabelMapper;
    @Mock
    private LabelPdfBuilder labelPdfBuilder;
    @Mock
    private AuditRecorder auditRecorder;

    @InjectMocks
    private LocationLabelService locationLabelService;

    private AssetLabelFormat activeFormat;
    private Location location1;
    private Location location2;

    @BeforeEach
    void setUp() {
        activeFormat = new AssetLabelFormat();
        activeFormat.setId(UUID.randomUUID());
        activeFormat.setCode("COMPACT");
        activeFormat.setActive(true);

        location1 = new Location();
        location1.setId(UUID.randomUUID());
        location1.setQrCode("LOC-ALG-000001");
        location1.setActive(true);

        location2 = new Location();
        location2.setId(UUID.randomUUID());
        location2.setQrCode("LOC-ALG-000002");
        location2.setActive(true);
    }

    @Test
    void generate_throwsWhenFormatNotFound() {
        when(formatRepository.findById(activeFormat.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationLabelService.generate(List.of(location1.getId()), activeFormat.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Format d'etiquette introuvable");
    }

    @Test
    void generate_throwsWhenFormatIsInactive() {
        activeFormat.setActive(false);
        when(formatRepository.findById(activeFormat.getId())).thenReturn(Optional.of(activeFormat));

        assertThatThrownBy(() -> locationLabelService.generate(List.of(location1.getId()), activeFormat.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("desactive");
    }

    @Test
    void generate_throwsWhenSomeLocationsAreNotFound() {
        when(formatRepository.findById(activeFormat.getId())).thenReturn(Optional.of(activeFormat));
        UUID missingId = UUID.randomUUID();
        when(locationRepository.findAllById(List.of(location1.getId(), missingId))).thenReturn(List.of(location1));

        assertThatThrownBy(() -> locationLabelService.generate(List.of(location1.getId(), missingId), activeFormat.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("introuvables");
    }

    @Test
    void generate_throwsWhenAnyLocationIsInactive_andNamesItsQrCodeInTheMessage() {
        location1.setActive(false);
        when(formatRepository.findById(activeFormat.getId())).thenReturn(Optional.of(activeFormat));
        when(locationRepository.findAllById(List.of(location1.getId()))).thenReturn(List.of(location1));

        assertThatThrownBy(() -> locationLabelService.generate(List.of(location1.getId()), activeFormat.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("desactive")
                .hasMessageContaining(location1.getQrCode());

        // Un local desactive bloque TOUTE la generation (pas seulement lui) :
        // le PDF ne doit jamais etre construit dans ce cas.
        verify(labelPdfBuilder, never()).build(anyList(), any());
    }

    @Test
    void generate_marksEveryLocationAsLabeled_savesOneHistoryEntryEach_andRecordsAudit() {
        when(formatRepository.findById(activeFormat.getId())).thenReturn(Optional.of(activeFormat));
        List<UUID> ids = List.of(location1.getId(), location2.getId());
        when(locationRepository.findAllById(ids)).thenReturn(List.of(location1, location2));
        when(labelPdfBuilder.build(anyList(), any())).thenReturn(new byte[]{1, 2, 3});
        lenient().when(locationLabelRepository.save(any(LocationLabel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        byte[] pdf = locationLabelService.generate(ids, activeFormat.getId());

        assertThat(pdf).containsExactly(1, 2, 3);
        assertThat(location1.isLabeled()).isTrue();
        assertThat(location2.isLabeled()).isTrue();

        ArgumentCaptor<LocationLabel> savedLabels = ArgumentCaptor.forClass(LocationLabel.class);
        verify(locationLabelRepository, times(2)).save(savedLabels.capture());
        assertThat(savedLabels.getAllValues())
                .extracting(LocationLabel::getLocation)
                .containsExactly(location1, location2);
        assertThat(savedLabels.getAllValues())
                .allSatisfy(label -> assertThat(label.getFormat()).isEqualTo(activeFormat));

        verify(auditRecorder, times(2)).record(
                eq(AuditAction.GENERATION_ETIQUETTE), eq("ETIQUETAGE"), eq("locations"), any(UUID.class), any(), any());
    }

    @Test
    void historyForLocation_throwsWhenLocationNotFound() {
        UUID id = UUID.randomUUID();
        when(locationRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> locationLabelService.historyForLocation(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historyForLocation_returnsMappedDtosInRepositoryOrder() {
        UUID id = location1.getId();
        when(locationRepository.existsById(id)).thenReturn(true);
        LocationLabel label = new LocationLabel();
        LocationLabelDto dto = new LocationLabelDto(UUID.randomUUID(), activeFormat.getId(), "COMPACT", "Format compact", "Sami Benreskallah", null);
        when(locationLabelRepository.findByLocationIdOrderByGeneratedAtDesc(id)).thenReturn(List.of(label));
        when(locationLabelMapper.toDto(label)).thenReturn(dto);

        List<LocationLabelDto> history = locationLabelService.historyForLocation(id);

        assertThat(history).containsExactly(dto);
    }
}
