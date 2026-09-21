package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.AbstractIntegrationTest;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression directe, sur une vraie base Postgres (Testcontainers), du bug
 * de collision de qr_code trouve pendant la verification live du
 * Checkpoint 1 "locaux scannables" (voir
 * claude/veco-assets-phase1-status.md, 21/09/2026) : la premiere version de
 * {@link LocationCodeGenerator} tirait d'une sequence PostgreSQL UNIQUE et
 * GLOBALE ({@code location_code_seq}), independante du backfill de V14 qui
 * numerote PAR SITE - les deux numerotations demarraient a 1 chacune de
 * leur cote, ce qui a provoque un doublon ({@code duplicate key value
 * violates unique constraint "uq_locations_qr_code"}) des la toute premiere
 * creation d'un local sur un site en ayant deja. Corrige par V15 (compteur
 * PAR SITE, {@code location_code_counters}, verrou de ligne).
 *
 * <p>Ce test-ci ne mocke rien (contrairement a {@link
 * LocationCodeGeneratorTest}) : ON CONFLICT DO NOTHING, le verrou de ligne
 * pris par l'UPDATE et son effet sous acces concurrent ne sont pas
 * verifiables avec un EntityManager mocke - seul un vrai Postgres le
 * permet.</p>
 */
class LocationCodeGeneratorIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger SITE_CODE_COUNTER = new AtomicInteger();

    @Autowired
    private LocationCodeGenerator locationCodeGenerator;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private Site siteAlger;
    private Site siteOran;

    @BeforeEach
    void setUp() {
        siteAlger = siteRepository.save(newSite("Alger"));
        siteOran = siteRepository.save(newSite("Oran"));
    }

    @Test
    void next_generatesSequentialCodesForTheSameSite() {
        String first = generateInOwnTransaction(siteAlger);
        String second = generateInOwnTransaction(siteAlger);
        String third = generateInOwnTransaction(siteAlger);

        assertThat(first).isEqualTo("LOC-" + siteAlger.getCode() + "-000001");
        assertThat(second).isEqualTo("LOC-" + siteAlger.getCode() + "-000002");
        assertThat(third).isEqualTo("LOC-" + siteAlger.getCode() + "-000003");
    }

    @Test
    void next_countersAreIndependentPerSite_neverCollide() {
        // C'est exactement le scenario du bug original : deux "premieres
        // creations" independantes (un site different chacune) obtiennent
        // toutes les deux le numero 1 sans jamais entrer en collision,
        // puisque le code est deja site-scope (LOC-{SITE}-...).
        String firstAlger = generateInOwnTransaction(siteAlger);
        String firstOran = generateInOwnTransaction(siteOran);

        assertThat(firstAlger).isEqualTo("LOC-" + siteAlger.getCode() + "-000001");
        assertThat(firstOran).isEqualTo("LOC-" + siteOran.getCode() + "-000001");
        assertThat(firstAlger).isNotEqualTo(firstOran);
    }

    @Test
    void next_continuesFromAPreExistingCounter_asItWouldAfterAMigrationBackfill() {
        // Reproduit precisement ce que fait le backfill de V15 pour un site
        // qui avait deja des locaux avant l'introduction du compteur par
        // site : la ligne location_code_counters est initialisee a la plus
        // haute valeur deja utilisee, PAS a 0 - le premier appel a next()
        // APRES coup doit continuer la numerotation, jamais repartir de 1
        // (c'est exactement l'absence de ce mecanisme qui causait le bug).
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                entityManager.createNativeQuery(
                                "insert into location_code_counters (site_id, last_value) values (:siteId, :lastValue)")
                        .setParameter("siteId", siteAlger.getId())
                        .setParameter("lastValue", 5L)
                        .executeUpdate());

        String next = generateInOwnTransaction(siteAlger);

        assertThat(next).isEqualTo("LOC-" + siteAlger.getCode() + "-000006");
    }

    @Test
    void next_underConcurrentCallsForTheSameSite_neverProducesADuplicate() throws Exception {
        int concurrentCreations = 20;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> tasks = IntStream.range(0, concurrentCreations)
                    .<Callable<String>>mapToObj(i -> () -> generateInOwnTransaction(siteAlger))
                    .toList();

            List<Future<String>> futures = executor.invokeAll(tasks, 30, TimeUnit.SECONDS);
            List<String> codes = new ArrayList<>();
            for (Future<String> future : futures) {
                codes.add(future.get());
            }

            Set<String> distinct = codes.stream().collect(Collectors.toSet());
            assertThat(distinct)
                    .as("aucun code QR de local ne doit jamais etre genere deux fois, meme sous acces concurrent sur le meme site")
                    .hasSize(concurrentCreations);
        } finally {
            executor.shutdown();
        }
    }

    // Chaque appel doit s'executer dans sa PROPRE transaction, comme il le
    // ferait reellement depuis LocationService.create() (une requete HTTP =
    // une transaction) : le verrou de ligne pris par l'UPDATE n'est libere
    // qu'au commit, donc enchainer plusieurs appels dans UNE SEULE
    // transaction de test se bloquerait sur lui-meme au lieu de serialiser
    // des appels reellement concurrents - et le test de concurrence
    // ci-dessus a justement besoin de vraies transactions separees pour
    // avoir un sens.
    private String generateInOwnTransaction(Site site) {
        return new TransactionTemplate(transactionManager)
                .execute(status -> locationCodeGenerator.next(site.getId(), site.getCode()));
    }

    private static Site newSite(String name) {
        Site site = new Site();
        site.setCode("S" + SITE_CODE_COUNTER.incrementAndGet());
        site.setName(name);
        return site;
    }
}
