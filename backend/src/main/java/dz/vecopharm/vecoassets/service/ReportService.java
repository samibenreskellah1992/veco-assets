package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.ReportFilter;
import dz.vecopharm.vecoassets.dto.ReportResultDto;
import dz.vecopharm.vecoassets.dto.ReportType;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetMovement;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import dz.vecopharm.vecoassets.entity.MovementStatus;
import dz.vecopharm.vecoassets.entity.MovementType;
import dz.vecopharm.vecoassets.repository.AssetMovementRepository;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import dz.vecopharm.vecoassets.specification.AssetSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Module /rapports (prompt maitre Phase 9) : 11 {@link ReportType}, tous
 * calcules a la demande (aucune table de rapport stockee) a partir des
 * memes entites que le reste de l'application. Chaque type ne retient que
 * les filtres de {@link ReportFilter} qui le concernent (documente ligne a
 * ligne ci-dessous) ; les autres sont silencieusement ignores.
 *
 * <p>Le resultat ({@link ReportResultDto}) porte des lignes deja formatees
 * en {@code String} (dates FR, montants a 2 decimales) : c'est la MEME
 * methode qui alimente a la fois l'affichage ecran et les trois formats
 * d'export (CSV/Excel/PDF, voir {@link ReportCsvExporter}/{@link
 * ReportExcelExporter}/{@link ReportPdfExporter}) - aucun risque de
 * divergence entre ce qui s'affiche et ce qui s'exporte.</p>
 */
@Service
public class ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONE = ZoneId.of("Africa/Algiers");

    private final AssetRepository assetRepository;
    private final AssetMovementRepository movementRepository;
    private final InventoryAnomalyRepository anomalyRepository;

    public ReportService(AssetRepository assetRepository, AssetMovementRepository movementRepository, InventoryAnomalyRepository anomalyRepository) {
        this.assetRepository = assetRepository;
        this.movementRepository = movementRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @Transactional(readOnly = true)
    public ReportResultDto generate(ReportType type, ReportFilter filter) {
        return switch (type) {
            case PAR_SITE -> breakdown(type, "Repartition par site", filteredAssets(filter), a -> a.getSite().getName());
            case PAR_CATEGORIE -> breakdown(type, "Repartition par categorie", filteredAssets(filter), a -> a.getCategory().getName());
            case PAR_ETAT -> breakdown(type, "Repartition par etat physique", filteredAssets(filter), a -> a.getCondition().name());
            case PAR_SERVICE -> parService(filter);
            case PAR_UTILISATEUR -> parUtilisateur(filter);
            case NON_ETIQUETEES -> nonEtiquetees(filter);
            case NON_INVENTORIEES -> nonInventoriees(filter);
            case ANOMALIES -> anomalies(filter);
            case MOUVEMENTS -> mouvements(type, "Mouvements", filter, null);
            case TRANSFERTS -> mouvements(type, "Transferts inter-site", filter, MovementType.TRANSFERT_INTER_SITE);
            case REFORMES -> reformes(filter);
        };
    }

    // --- Repartitions ("par ...") --------------------------------------------------

    /** Filtres pertinents pour toutes les repartitions et listes basees sur les immobilisations : site/categorie/etat/statut. */
    private List<Asset> filteredAssets(ReportFilter filter) {
        var spec = AssetSpecification.withFilters(false, filter.siteId(), filter.categoryId(), null, filter.condition(), filter.status(), null, null);
        return assetRepository.findAll(spec);
    }

    private ReportResultDto breakdown(ReportType type, String title, List<Asset> assets, java.util.function.Function<Asset, String> classifier) {
        Map<String, List<Asset>> grouped = assets.stream()
                .collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()));
        List<List<String>> rows = grouped.entrySet().stream()
                .map(e -> List.of(e.getKey(), String.valueOf(e.getValue().size()), amount(sumValue(e.getValue()))))
                .sorted(Comparator.comparing(r -> r.get(0)))
                .toList();
        return new ReportResultDto(type, title, Instant.now(), List.of("Libelle", "Nb immobilisations", "Valeur d'acquisition (DZD)"), rows);
    }

    private ReportResultDto parService(ReportFilter filter) {
        Map<String, List<Asset>> grouped = filteredAssets(filter).stream()
                .collect(Collectors.groupingBy(ReportService::serviceKey, LinkedHashMap::new, Collectors.toList()));
        List<List<String>> rows = grouped.entrySet().stream()
                .map(e -> List.of(e.getKey(), String.valueOf(e.getValue().size())))
                .sorted(Comparator.comparing(r -> r.get(0)))
                .toList();
        return new ReportResultDto(ReportType.PAR_SERVICE, "Repartition par direction / departement / service", Instant.now(),
                List.of("Direction / departement / service", "Nb immobilisations"), rows);
    }

    private static String serviceKey(Asset a) {
        String direction = orDash(a.getDirection());
        String department = orDash(a.getDepartment());
        String service = orDash(a.getService());
        if ("-".equals(direction) && "-".equals(department) && "-".equals(service)) {
            return "Non renseigne";
        }
        return direction + " / " + department + " / " + service;
    }

    private ReportResultDto parUtilisateur(ReportFilter filter) {
        // Regroupe par id utilisateur (pas par nom, pour ne jamais confondre deux
        // utilisateurs homonymes) - le nom affiche est repris du premier element.
        Map<java.util.UUID, List<Asset>> grouped = filteredAssets(filter).stream()
                .filter(a -> a.getCurrentUser() != null)
                .collect(Collectors.groupingBy(a -> a.getCurrentUser().getId(), LinkedHashMap::new, Collectors.toList()));
        List<List<String>> rows = grouped.values().stream()
                .map(list -> {
                    Asset first = list.get(0);
                    String siteName = first.getCurrentUser().getSite() != null ? first.getCurrentUser().getSite().getName() : "-";
                    return List.of(first.getCurrentUser().getFullName(), siteName, String.valueOf(list.size()));
                })
                .sorted(Comparator.comparing(r -> r.get(0)))
                .toList();
        return new ReportResultDto(ReportType.PAR_UTILISATEUR, "Repartition par utilisateur affecte", Instant.now(),
                List.of("Utilisateur", "Site", "Nb immobilisations affectees"), rows);
    }

    // --- Listes ----------------------------------------------------------------

    private ReportResultDto nonEtiquetees(ReportFilter filter) {
        List<List<String>> rows = filteredAssets(filter).stream()
                .filter(a -> !a.isLabeled())
                .sorted(Comparator.comparing(Asset::getAssetCode))
                .map(a -> List.of(a.getAssetCode(), a.getDesignation(), a.getSite().getName(), a.getCategory().getName(), a.getStatus().name()))
                .toList();
        return new ReportResultDto(ReportType.NON_ETIQUETEES, "Immobilisations non etiquetees", Instant.now(),
                List.of("Code", "Designation", "Site", "Categorie", "Statut"), rows);
    }

    private ReportResultDto nonInventoriees(ReportFilter filter) {
        List<List<String>> rows = filteredAssets(filter).stream()
                .filter(a -> a.getLastInventoryAt() == null)
                .sorted(Comparator.comparing(Asset::getAssetCode))
                .map(a -> List.of(a.getAssetCode(), a.getDesignation(), a.getSite().getName(), a.getCategory().getName(),
                        a.getStatus().name(), date(a.getCreatedAt())))
                .toList();
        return new ReportResultDto(ReportType.NON_INVENTORIEES, "Immobilisations jamais inventoriees", Instant.now(),
                List.of("Code", "Designation", "Site", "Categorie", "Statut", "Entree en parc le"), rows);
    }

    private ReportResultDto reformes(ReportFilter filter) {
        List<Asset> reformed = filteredAssets(filter).stream()
                .filter(a -> a.getStatus() == AssetStatus.REFORME)
                .toList();
        // Le mouvement REFORME execute le plus recent porte la date/le motif reel - toute reforme
        // realisee depuis la Phase 8 passe obligatoirement par ce workflow (execute() est le seul
        // point qui ecrit AssetStatus.REFORME). Exception verifiee en base : l'immobilisation demo
        // deja au statut REFORME dans le jeu de seed (Phase 2, VECO-IMM-000012, avant meme que ce
        // workflow existe) n'a PAS de mouvement correspondant - d'ou le fallback "-" ci-dessous,
        // indispensable et non theorique (confirme lors de la verification SQL de cette phase).
        Map<java.util.UUID, AssetMovement> lastReformeByAsset = movementRepository.findAllByOrderByRequestedAtDesc().stream()
                .filter(m -> m.getMovementType() == MovementType.REFORME && m.getStatus() == MovementStatus.EXECUTE)
                .collect(Collectors.toMap(m -> m.getAsset().getId(), m -> m, (first, second) -> first));

        List<List<String>> rows = reformed.stream()
                .sorted(Comparator.comparing(Asset::getAssetCode))
                .map(a -> {
                    AssetMovement movement = lastReformeByAsset.get(a.getId());
                    String reformDate = movement != null ? datetime(movement.getExecutedAt()) : "-";
                    String reason = movement != null && movement.getReason() != null && !movement.getReason().isBlank() ? movement.getReason() : "-";
                    return List.of(a.getAssetCode(), a.getDesignation(), a.getSite().getName(), a.getCategory().getName(), reformDate, reason);
                })
                .toList();
        return new ReportResultDto(ReportType.REFORMES, "Immobilisations reformees", Instant.now(),
                List.of("Code", "Designation", "Site", "Categorie", "Date de reforme", "Motif"), rows);
    }

    private ReportResultDto anomalies(ReportFilter filter) {
        List<List<String>> rows = anomalyRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> filter.anomalyStatus() == null || a.getStatus() == filter.anomalyStatus())
                .filter(a -> filter.siteId() == null || (a.getAsset() != null && a.getAsset().getSite().getId().equals(filter.siteId())))
                .filter(a -> inDateRange(a.getCreatedAt(), filter))
                .map(this::anomalyRow)
                .toList();
        return new ReportResultDto(ReportType.ANOMALIES, "Anomalies d'inventaire", Instant.now(),
                List.of("Date", "Campagne", "Immobilisation", "Type", "Statut", "Description", "Signalee par"), rows);
    }

    private List<String> anomalyRow(InventoryAnomaly a) {
        return List.of(
                datetime(a.getCreatedAt()),
                a.getCampaign().getName(),
                a.getAsset() != null ? a.getAsset().getAssetCode() + " - " + a.getAsset().getDesignation() : "-",
                a.getAnomalyType().name(),
                a.getStatus().name(),
                orDash(a.getDescription()),
                a.getReportedBy() != null ? a.getReportedBy().getFullName() : "-"
        );
    }

    private ReportResultDto mouvements(ReportType type, String title, ReportFilter filter, MovementType forcedType) {
        List<List<String>> rows = movementRepository.findAllByOrderByRequestedAtDesc().stream()
                .filter(m -> forcedType == null || m.getMovementType() == forcedType)
                .filter(m -> filter.movementType() == null || m.getMovementType() == filter.movementType())
                .filter(m -> filter.siteId() == null
                        || m.getAsset().getSite().getId().equals(filter.siteId())
                        || (m.getFromSite() != null && m.getFromSite().getId().equals(filter.siteId()))
                        || (m.getToSite() != null && m.getToSite().getId().equals(filter.siteId())))
                .filter(m -> inDateRange(m.getRequestedAt(), filter))
                .map(this::movementRow)
                .toList();
        List<String> columns = forcedType == MovementType.TRANSFERT_INTER_SITE
                ? List.of("Date demande", "Immobilisation", "Site origine", "Site destination", "Statut", "Execute le")
                : List.of("Date demande", "Immobilisation", "Type", "Statut", "Demande par", "Valide par", "Execute le");
        return new ReportResultDto(type, title, Instant.now(), columns, rows);
    }

    private List<String> movementRow(AssetMovement m) {
        String assetLabel = m.getAsset().getAssetCode() + " - " + m.getAsset().getDesignation();
        if (m.getMovementType() == MovementType.TRANSFERT_INTER_SITE) {
            return List.of(
                    datetime(m.getRequestedAt()),
                    assetLabel,
                    m.getFromSite() != null ? m.getFromSite().getName() : "-",
                    m.getToSite() != null ? m.getToSite().getName() : "-",
                    m.getStatus().name(),
                    m.getExecutedAt() != null ? datetime(m.getExecutedAt()) : "-"
            );
        }
        return List.of(
                datetime(m.getRequestedAt()),
                assetLabel,
                m.getMovementType().name(),
                m.getStatus().name(),
                m.getRequestedBy() != null ? m.getRequestedBy().getFullName() : "-",
                m.getValidatedBy() != null ? m.getValidatedBy().getFullName() : "-",
                m.getExecutedAt() != null ? datetime(m.getExecutedAt()) : "-"
        );
    }

    // --- Utilitaires de formatage ------------------------------------------------

    private static BigDecimal sumValue(List<Asset> assets) {
        return assets.stream().map(Asset::getAcquisitionValue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String amount(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String date(Instant instant) {
        return instant == null ? "-" : DATE_FMT.format(instant.atZone(ZONE));
    }

    private static String datetime(Instant instant) {
        return instant == null ? "-" : DATETIME_FMT.format(instant.atZone(ZONE));
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static boolean inDateRange(Instant instant, ReportFilter filter) {
        if (instant == null || (filter.dateFrom() == null && filter.dateTo() == null)) {
            return true;
        }
        LocalDate date = instant.atZone(ZONE).toLocalDate();
        if (filter.dateFrom() != null && date.isBefore(filter.dateFrom())) {
            return false;
        }
        return filter.dateTo() == null || !date.isAfter(filter.dateTo());
    }
}
