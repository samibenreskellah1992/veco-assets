package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.repository.SettingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Genere le code QR d'un nouveau local a partir du parametre administrable
 * {@code location_code.format} (V14, meme convention que
 * {@code asset_code.format} - voir {@link AssetCodeGenerator}) et d'un
 * compteur PAR SITE ({@code location_code_counters}, V15). Le format local
 * attend deux specificateurs {@link String#format} : le code du site
 * ({@code %s}) puis le numero de sequence propre a ce site ({@code %06d}) -
 * le code local est ainsi site-scope ({@code LOC-{SITE}-{SEQUENCE}}).
 *
 * <p>Historique (V14 -&gt; V15) : la premiere version tirait d'une sequence
 * PostgreSQL UNIQUE et GLOBALE ({@code location_code_seq}), independante de
 * la numerotation par site utilisee par le backfill de V14. Les deux
 * numerotations demarraient a 1 chacune de leur cote, ce qui provoquait des
 * doublons de qr_code des la premiere creation sur un site ayant deja des
 * locaux. Remplace par un compteur par site (table
 * {@code location_code_counters}), initialise a la valeur la plus haute
 * deja utilisee par le backfill pour ce site.</p>
 *
 * <p>Comme {@link AssetCodeGenerator}, un parametre de format absent ou mal
 * forme retombe silencieusement sur le format par defaut plutot que de
 * bloquer la creation d'un local.</p>
 */
@Component
public class LocationCodeGenerator {

    private static final String FORMAT_SETTING_KEY = "location_code.format";
    private static final String DEFAULT_FORMAT = "LOC-%s-%06d";

    private final SettingRepository settingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public LocationCodeGenerator(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * Incremente atomiquement le compteur du site {@code siteId} et renvoie
     * le code QR genere. Le compteur est initialise a 0 au premier appel
     * pour un site (ON CONFLICT DO NOTHING), puis incremente sous verrou de
     * ligne : l'UPDATE prend un verrou sur la ligne du site jusqu'au commit
     * de la transaction courante, ce qui serialise les creations
     * concurrentes de locaux sur un meme site et garantit que le SELECT qui
     * suit, dans la meme transaction, lit bien sa propre increment.
     */
    @Transactional
    public String next(UUID siteId, String siteCode) {
        entityManager.createNativeQuery(
                        "insert into location_code_counters (site_id, last_value) values (:siteId, 0) "
                                + "on conflict (site_id) do nothing")
                .setParameter("siteId", siteId)
                .executeUpdate();

        entityManager.createNativeQuery(
                        "update location_code_counters set last_value = last_value + 1 where site_id = :siteId")
                .setParameter("siteId", siteId)
                .executeUpdate();

        long sequenceValue = ((Number) entityManager
                .createNativeQuery("select last_value from location_code_counters where site_id = :siteId")
                .setParameter("siteId", siteId)
                .getSingleResult()).longValue();

        String format = settingRepository.findByKey(FORMAT_SETTING_KEY)
                .map(setting -> setting.getValue())
                .filter(value -> value != null && !value.isBlank())
                .orElse(DEFAULT_FORMAT);

        try {
            return String.format(format, siteCode, sequenceValue);
        } catch (RuntimeException ex) {
            // Parametre "location_code.format" invalide (ex. specificateurs
            // incompatibles) : on ne bloque jamais la creation pour autant.
            return String.format(DEFAULT_FORMAT, siteCode, sequenceValue);
        }
    }
}
