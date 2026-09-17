package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.ReportResultDto;
import dz.vecopharm.vecoassets.dto.ReportType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification "de bout en bout" de l'export Excel reel (Phase 9, Apache
 * POI) : contrairement a ReportServiceTest (logique de calcul des
 * indicateurs, mockee), ce test genere un vrai classeur .xlsx, le rouvre
 * avec POI et verifie que le titre, les en-tetes de colonnes et les lignes
 * de donnees sont exactement ceux du ReportResultDto d'entree. C'etait,
 * avec la generation des etiquettes QR/PDF (voir LabelGenerationContentTest),
 * le seul point du backend qui n'avait jamais ete verifie que par revue de
 * code plutot que par execution reelle (voir docs/ROADMAP.md section 13).
 *
 * Le fichier genere est aussi ecrit dans target/verification-output/ pour
 * une ouverture manuelle dans Excel/LibreOffice.
 */
class ReportExcelExporterContentTest {

    private final ReportExcelExporter exporter = new ReportExcelExporter();

    @Test
    void exportedWorkbookContainsExactTitleHeadersAndRows() throws IOException {
        ReportResultDto report = new ReportResultDto(
                ReportType.PAR_SITE,
                "Repartition par site",
                Instant.parse("2026-09-17T09:00:00Z"),
                List.of("Site", "Nombre d'immobilisations", "Valeur d'acquisition totale"),
                List.of(
                        List.of("Siege Alger", "128", "45 320 000,00 DZD"),
                        List.of("Depot Blida", "54", "12 980 500,00 DZD")
                )
        );

        byte[] xlsxBytes = exporter.export(report);
        writeVerificationFile("rapport-par-site.xlsx", xlsxBytes);

        // Un .xlsx est une archive ZIP : signature "PK" attendue en tete de fichier.
        assertThat(xlsxBytes[0]).isEqualTo((byte) 'P');
        assertThat(xlsxBytes[1]).isEqualTo((byte) 'K');

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Repartition par site");

            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Repartition par site");
            assertThat(sheet.getMergedRegions()).containsExactly(new CellRangeAddress(0, 0, 0, 2));

            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("2 ligne(s)");

            Row headerRow = sheet.getRow(3);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Site");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Nombre d'immobilisations");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Valeur d'acquisition totale");

            Row dataRow1 = sheet.getRow(4);
            assertThat(dataRow1.getCell(0).getStringCellValue()).isEqualTo("Siege Alger");
            assertThat(dataRow1.getCell(1).getStringCellValue()).isEqualTo("128");
            assertThat(dataRow1.getCell(2).getStringCellValue()).isEqualTo("45 320 000,00 DZD");

            Row dataRow2 = sheet.getRow(5);
            assertThat(dataRow2.getCell(0).getStringCellValue()).isEqualTo("Depot Blida");

            // Ligne d'indice 2 (ligne vide de separation) jamais instanciee par
            // ReportExcelExporter (rowIndex incremente sans sheet.createRow) :
            // seules 5 lignes physiques existent (0, 1, 3, 4, 5).
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(5);
        }
    }

    @Test
    void sheetNameIsSanitizedAndTruncatedTo31Characters() throws IOException {
        String longTitleWithForbiddenChars = "Rapport: Mouvements / Transferts [Site: Alger*Blida?] 2026";
        ReportResultDto report = new ReportResultDto(
                ReportType.MOUVEMENTS,
                longTitleWithForbiddenChars,
                Instant.now(),
                List.of("Colonne"),
                List.of()
        );

        byte[] xlsxBytes = exporter.export(report);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes))) {
            String sheetName = workbook.getSheetAt(0).getSheetName();
            assertThat(sheetName).hasSizeLessThanOrEqualTo(31);
            assertThat(sheetName).doesNotContain("/", "\\", "*", "?", "[", "]", ":");
        }
    }

    private static void writeVerificationFile(String filename, byte[] content) throws IOException {
        Path dir = Path.of("target", "verification-output");
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), content);
    }
}
