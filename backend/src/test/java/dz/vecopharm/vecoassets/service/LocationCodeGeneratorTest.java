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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (Mockito) de la logique de repli de format de
 * {@link LocationCodeGenerator} - meme esprit et meme structure que
 * {@link AssetCodeGeneratorTest} : le compteur par site lui-meme (table
 * location_code_counters, ON CONFLICT, verrou de ligne) n'est pas testable
 * de facon significative avec un EntityManager mocke (voir
 * {@link LocationCodeGeneratorIntegrationTest} pour ca, avec une vraie
 * base) ; ce test-ci couvre le risque reel cote administrateur - un
 * parametre "location_code.format" absent, vide ou mal forme ne doit
 * jamais bloquer la creation d'un local (meme discipline que
 * AssetCodeGenerator pour "asset_code.format").
 */
@ExtendWith(MockitoExtension.class)
class LocationCodeGeneratorTest {

    private static final String FORMAT_KEY = "location_code.format";
    private static final UUID SITE_ID = UUID.randomUUID();
    private static final String SITE_CODE = "ALG";

    @Mock
    private SettingRepository settingRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    @InjectMocks
    private LocationCodeGenerator locationCodeGenerator;

    @BeforeEach
    void setUp() {
        // Meme cause que AssetCodeGeneratorTest : @PersistenceContext EntityManager
        // est injecte par champ, pas par le constructeur (qui ne prend que
        // SettingRepository) - @InjectMocks de Mockito choisit le constructeur et
        // n'injecte alors plus aucun champ, laissant entityManager a null.
        ReflectionTestUtils.setField(locationCodeGenerator, "entityManager", entityManager);

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(7L);
    }

    @Test
    void next_usesDefaultFormatWhenNoSettingConfigured() {
        when(settingRepository.findByKey(FORMAT_KEY)).thenReturn(Optional.empty());

        assertThat(locationCodeGenerator.next(SITE_ID, SITE_CODE)).isEqualTo("LOC-ALG-000007");
    }

    @Test
    void next_usesConfiguredFormatWhenValid() {
        when(settingRepository.findByKey(FORMAT_KEY)).thenReturn(Optional.of(settingValued("L-%s-%04d")));

        assertThat(locationCodeGenerator.next(SITE_ID, SITE_CODE)).isEqualTo("L-ALG-0007");
    }

    @Test
    void next_fallsBackToDefaultFormatWhenConfiguredValueIsBlank() {
        when(settingRepository.findByKey(FORMAT_KEY)).thenReturn(Optional.of(settingValued("   ")));

        assertThat(locationCodeGenerator.next(SITE_ID, SITE_CODE)).isEqualTo("LOC-ALG-000007");
    }

    @Test
    void next_fallsBackToDefaultFormatWhenConfiguredFormatIsMalformed() {
        // "%q" n'est pas un specificateur de conversion valide - String.format
        // leve UnknownFormatConversionException (une RuntimeException), le cas
        // que next() doit absorber sans jamais bloquer la creation d'un local.
        when(settingRepository.findByKey(FORMAT_KEY)).thenReturn(Optional.of(settingValued("LOC-%q-INVALID")));

        assertThat(locationCodeGenerator.next(SITE_ID, SITE_CODE)).isEqualTo("LOC-ALG-000007");
    }

    private static Setting settingValued(String value) {
        Setting setting = new Setting();
        setting.setKey(FORMAT_KEY);
        setting.setValue(value);
        return setting;
    }
}
