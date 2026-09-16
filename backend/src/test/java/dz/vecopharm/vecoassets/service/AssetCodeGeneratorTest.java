package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.entity.Setting;
import dz.vecopharm.vecoassets.repository.SettingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito) de {@link AssetCodeGenerator}. La sequence
 * PostgreSQL elle-meme n'est pas testable sans base (mockee ici), mais la
 * logique reellement risquee - le repli sur le format par defaut quand le
 * parametre administrable {@code asset_code.format} est absent, vide, ou
 * mal forme - l'est integralement : un administrateur qui se trompe dans ce
 * parametre ne doit jamais bloquer la creation d'immobilisations.
 */
@ExtendWith(MockitoExtension.class)
class AssetCodeGeneratorTest {

    private static final String FORMAT_KEY = "asset_code.format";

    @Mock
    private SettingRepository settingRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    @InjectMocks
    private AssetCodeGenerator assetCodeGenerator;

    @BeforeEach
    void setUp() {
        when(entityManager.createNativeQuery("select nextval('asset_code_seq')")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(42L);
    }

    @Test
    void next_usesDefaultFormatWhenNoSettingConfigured() {
        when(settingRepository.findByKey(FORMAT_KEY)).thenReturn(Optional.empty());

        assertThat(assetCodeGenerator.next()).isEqualTo("VECO-IMM-000042");
    }

    @Test
    void next_usesConfiguredFormatWhenValid() {
        when(settingRepository.findByKey(FORMAT_KEY)).thenReturn(Optional.of(settingValued("MAT-%04d")));

        assertThat(assetCodeGenerator.next()).isEqualTo("MAT-0042");
    }

    @Test
    void next_fallsBackToDefaultFormatWhenConfiguredValueIsBlank() {
        when(settingRepository.findByKey(FORMAT_KEY)).thenReturn(Optional.of(settingValued("   ")));

        assertThat(assetCodeGenerator.next()).isEqualTo("VECO-IMM-000042");
    }

    @Test
    void next_fallsBackToDefaultFormatWhenConfiguredFormatIsMalformed() {
        // "%q" n'est pas un specificateur de conversion valide - String.format
        // leve UnknownFormatConversionException (une RuntimeException), le cas
        // que next() doit absorber sans jamais bloquer la creation.
        when(settingRepository.findByKey(FORMAT_KEY)).thenReturn(Optional.of(settingValued("VECO-%q-INVALID")));

        assertThat(assetCodeGenerator.next()).isEqualTo("VECO-IMM-000042");
    }

    private static Setting settingValued(String value) {
        Setting setting = new Setting();
        setting.setKey(FORMAT_KEY);
        setting.setValue(value);
        return setting;
    }
}
