package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.ReportResultDto;
import dz.vecopharm.vecoassets.dto.ReportType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification "de bout en bout" de l'export PDF tabulaire d'un rapport
 * (Phase 9, PDFBox) : le dernier des trois exports de rapport (CSV/Excel/PDF)
 * encore verifie uniquement par revue de code apres la passe du 17/09/2026
 * sur LabelGenerationContentTest et ReportExcelExporterContentTest (voir ces
 * classes). Ce test genere un vrai PDF, en extrait le texte reel
 * (PDFTextStripper) et verifie que le titre, les en-tetes de colonnes et
 * chaque valeur de chaque ligne sont bien presents - y compris a travers la
 * pagination automatique (en-tete de tableau et pied de page "Page X / N"
 * repetes sur chaque page, aucune ligne perdue ni dupliquee entre deux
 * pages).
 *
 * Le fichier genere est aussi ecrit dans target/verification-output/ pour
 * une ouverture manuelle dans n'importe quel lecteur PDF.
 */
class ReportPdfExporterContentTest {

    private final ReportPdfExporter exporter = new ReportPdfExporter();

    @Test
    void exportedPdfContainsTitleColumnsAndAllRowValues() throws IOException {
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

        byte[] pdfBytes = exporter.export(report);
        writeVerificationFile("rapport-par-site.pdf", pdfBytes);

        assertThat(new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);

            String text = extractText(document);
            assertThat(text).contains("Repartition par site");
            assertThat(text).contains("Site", "Nombre d'immobilisations", "Valeur d'acquisition totale");
            assertThat(text).contains("Siege Alger", "128", "45 320 000,00 DZD");
            assertThat(text).contains("Depot Blida", "54", "12 980 500,00 DZD");
            assertThat(text).contains("2 ligne(s)");
            assertThat(text).contains("Page 1 / 1");
        }
    }

    @Test
    void paginatesAcrossMultiplePagesWithoutLosingOrDuplicatingRows() throws IOException {
        int rowCount = 120;
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            rows.add(List.of(String.format("IMMO-%04d", i), "Materiel " + i, "Site " + (i % 5)));
        }
        ReportResultDto report = new ReportResultDto(
                ReportType.MOUVEMENTS,
                "Liste de tous les mouvements",
                Instant.now(),
                List.of("Code", "Designation", "Site"),
                rows
        );

        byte[] pdfBytes = exporter.export(report);
        writeVerificationFile("rapport-pagine.pdf", pdfBytes);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalPages = document.getNumberOfPages();
            assertThat(totalPages).as("un rapport de 120 lignes doit forcement se paginer sur plusieurs pages A4").isGreaterThan(1);

            String fullText = extractText(document);

            // Chaque code immobilisation genere doit apparaitre exactement une
            // fois dans le document entier : ni perdu entre deux pages, ni
            // duplique par la logique de pagination.
            for (int i = 0; i < rowCount; i++) {
                String code = String.format("IMMO-%04d", i);
                Matcher matcher = Pattern.compile(Pattern.quote(code)).matcher(fullText);
                int occurrences = 0;
                while (matcher.find()) {
                    occurrences++;
                }
                assertThat(occurrences).as("occurrences de " + code + " dans le PDF").isEqualTo(1);
            }

            // Chaque page doit porter son propre pied de page "Page X / N", et
            // l'en-tete de colonnes doit etre repete sur chaque page.
            for (int page = 1; page <= totalPages; page++) {
                String pageText = extractText(document, page, page);
                assertThat(pageText).contains("Page " + page + " / " + totalPages);
                assertThat(pageText).contains("Code", "Designation", "Site");
            }
        }
    }

    @Test
    void emptyReportRendersFallbackMessageOnSinglePage() throws IOException {
        ReportResultDto report = new ReportResultDto(
                ReportType.NON_INVENTORIEES,
                "Immobilisations jamais inventoriees",
                Instant.now(),
                List.of("Code", "Designation"),
                List.of()
        );

        byte[] pdfBytes = exporter.export(report);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);

            String text = extractText(document);
            assertThat(text).contains("Aucune donnee pour les filtres selectionnes.");
            assertThat(text).contains("0 ligne(s)");
            assertThat(text).contains("Page 1 / 1");
        }
    }

    private static String extractText(PDDocument document) throws IOException {
        return new PDFTextStripper().getText(document);
    }

    private static String extractText(PDDocument document, int startPage, int endPage) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(startPage);
        stripper.setEndPage(endPage);
        return stripper.getText(document);
    }

    private static void writeVerificationFile(String filename, byte[] content) throws IOException {
        Path dir = Path.of("target", "verification-output");
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), content);
    }
}
