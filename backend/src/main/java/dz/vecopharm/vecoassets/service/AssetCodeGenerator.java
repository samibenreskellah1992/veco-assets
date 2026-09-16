package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.repository.SettingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genere le code d'une nouvelle immobilisation a partir du parametre
 * administrable {@code asset_code.format} (prompt maitre section 53 - ex.
 * {@code VECO-IMM-%06d}, compatible {@link String#format}) et de la
 * sequence PostgreSQL {@code asset_code_seq} (V9). La sequence garantit
 * l'unicite et la progression meme en cas de creations concurrentes -
 * jamais un simple {@code count(*) + 1} qui se reutiliserait apres une
 * suppression.
 *
 * Si le parametre est absent ou mal forme, on retombe sur le format par
 * defaut plutot que de bloquer toute creation d'immobilisation : un
 * administrateur peut corriger le parametre sans redeploiement, mais une
 * valeur invalide ne doit jamais empecher l'usage courant de
 * l'application.
 */
@Component
public class AssetCodeGenerator {

    private static final String FORMAT_SETTING_KEY = "asset_code.format";
    private static final String DEFAULT_FORMAT = "VECO-IMM-%06d";

    private final SettingRepository settingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AssetCodeGenerator(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional
    public String next() {
        long sequenceValue = ((Number) entityManager
                .createNativeQuery("select nextval('asset_code_seq')")
                .getSingleResult()).longValue();

        String format = settingRepository.findByKey(FORMAT_SETTING_KEY)
                .map(setting -> setting.getValue())
                .filter(value -> value != null && !value.isBlank())
                .orElse(DEFAULT_FORMAT);

        try {
            return String.format(format, sequenceValue);
        } catch (RuntimeException ex) {
            // Parametre "asset_code.format" invalide (ex. specificateur non
            // numerique) : on ne bloque jamais la creation pour autant.
            return String.format(DEFAULT_FORMAT, sequenceValue);
        }
    }
}
