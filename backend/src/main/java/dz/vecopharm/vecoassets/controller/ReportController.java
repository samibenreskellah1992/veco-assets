package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.ExportFormat;
import dz.vecopharm.vecoassets.dto.ReportFilter;
import dz.vecopharm.vecoassets.dto.ReportResultDto;
import dz.vecopharm.vecoassets.dto.ReportType;
import dz.vecopharm.vecoassets.entity.AnomalyStatus;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.MovementType;
import dz.vecopharm.vecoassets.service.ReportCsvExporter;
import dz.vecopharm.vecoassets.service.ReportExcelExporter;
import dz.vecopharm.vecoassets.service.ReportPdfExporter;
import dz.vecopharm.vecoassets.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Module /rapports (prompt maitre Phase 9). Lecture reservee a REPORT_VIEW,
 * export (CSV/Excel/PDF, susceptible d'etre diffuse hors de l'application)
 * a REPORT_EXPORT - deux permissions distinctes deja seedees depuis la
 * Phase 2 (RESPONSABLE_SITE/RESPONSABLE_SERVICE/CONSULTATION peuvent
 * consulter les rapports mais pas les exporter, seuls ADMIN et
 * GESTIONNAIRE_PATRIMOINE le peuvent - voir V3__roles_permissions_users.sql).
 * Les 8 parametres de filtre optionnels sont repetes explicitement sur les
 * deux endpoints (meme convention que le reste du controller layer - voir
 * {@code AssetController} - plutot qu'un objet de parametres implicite).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportCsvExporter csvExporter;
    private final ReportExcelExporter excelExporter;
    private final ReportPdfExporter pdfExporter;

    public ReportController(
            ReportService reportService,
            ReportCsvExporter csvExporter,
            ReportExcelExporter excelExporter,
            ReportPdfExporter pdfExporter
    ) {
        this.reportService = reportService;
        this.csvExporter = csvExporter;
        this.excelExporter = excelExporter;
        this.pdfExporter = pdfExporter;
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ReportResultDto get(
            @PathVariable ReportType type,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) AssetCondition condition,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) AnomalyStatus anomalyStatus,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo
    ) {
        return reportService.generate(type, new ReportFilter(siteId, categoryId, condition, status, movementType, anomalyStatus, dateFrom, dateTo));
    }

    /**
     * Reutilise {@link ReportService#generate} pour calculer exactement les
     * memes lignes que celles affichees a l'ecran (voir javadoc {@link
     * dz.vecopharm.vecoassets.dto.ReportResultDto}), puis les passe a
     * l'exporteur du format demande - aucune logique metier dupliquee ici.
     */
    @GetMapping("/{type}/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> export(
            @PathVariable ReportType type,
            @RequestParam ExportFormat format,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) AssetCondition condition,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) AnomalyStatus anomalyStatus,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo
    ) {
        ReportFilter filter = new ReportFilter(siteId, categoryId, condition, status, movementType, anomalyStatus, dateFrom, dateTo);
        ReportResultDto report = reportService.generate(type, filter);
        byte[] content = switch (format) {
            case CSV -> csvExporter.export(report);
            case XLSX -> excelExporter.export(report);
            case PDF -> pdfExporter.export(report);
        };
        MediaType mediaType = switch (format) {
            case CSV -> MediaType.parseMediaType("text/csv");
            case XLSX -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case PDF -> MediaType.APPLICATION_PDF;
        };
        String extension = switch (format) {
            case CSV -> "csv";
            case XLSX -> "xlsx";
            case PDF -> "pdf";
        };
        String filename = "rapport-" + type.name().toLowerCase() + "-" + LocalDate.now() + "." + extension;
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }
}
