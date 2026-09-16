package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.ReportResultDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Export PDF tabulaire d'un rapport (prompt maitre Phase 9), via PDFBox
 * (deja une dependance depuis la Phase 6 - Etiquetage, aucun risque de
 * compilation supplementaire). Contrairement a {@link
 * dz.vecopharm.vecoassets.service.LabelPdfBuilder} (une page par
 * immobilisation, taille fixee par le format d'etiquette), ce constructeur
 * pagine un tableau generique sur autant de pages A4 que necessaire, avec
 * l'en-tete de colonnes repete sur chaque page.
 */
@Component
public class ReportPdfExporter {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 36f;
    private static final float ROW_HEIGHT = 16f;
    private static final float HEADER_BLOCK_HEIGHT = 60f;
    private static final float FOOTER_HEIGHT = 20f;
    private static final float FONT_SIZE = 8f;
    private static final float CELL_PADDING = 3f;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Africa/Algiers"));

    public byte[] export(ReportResultDto report) {
        try (PDDocument document = new PDDocument()) {
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float contentWidth = PAGE_WIDTH - 2 * MARGIN;
            float[] columnWidths = computeColumnWidths(report, contentWidth);

            float tableTop = PAGE_HEIGHT - MARGIN - HEADER_BLOCK_HEIGHT;
            float usableHeight = tableTop - MARGIN - FOOTER_HEIGHT - ROW_HEIGHT; // - la ligne d'en-tete de colonnes
            int rowsPerPage = Math.max(1, (int) (usableHeight / ROW_HEIGHT));

            List<List<String>> rows = report.rows();
            int totalPages = rows.isEmpty() ? 1 : (int) Math.ceil(rows.size() / (double) rowsPerPage);

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                int from = pageIndex * rowsPerPage;
                int to = Math.min(from + rowsPerPage, rows.size());

                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    float cursorY = PAGE_HEIGHT - MARGIN;
                    if (pageIndex == 0) {
                        cursorY = drawHeaderBlock(stream, bold, regular, report, cursorY);
                    } else {
                        cursorY -= 10f;
                    }
                    cursorY = drawTableHeaderRow(stream, bold, report.columns(), columnWidths, cursorY);
                    for (int r = from; r < to; r++) {
                        drawTableRow(stream, regular, rows.get(r), columnWidths, cursorY);
                        cursorY -= ROW_HEIGHT;
                    }
                    if (rows.isEmpty() && pageIndex == 0) {
                        drawText(stream, regular, FONT_SIZE + 1, "Aucune donnee pour les filtres selectionnes.", MARGIN, cursorY - ROW_HEIGHT);
                    }
                    drawFooter(stream, regular, pageIndex + 1, totalPages);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Echec de generation du PDF du rapport", ex);
        }
    }

    private float drawHeaderBlock(PDPageContentStream stream, PDFont bold, PDFont regular, ReportResultDto report, float cursorY) throws IOException {
        drawText(stream, bold, 14f, sanitize(report.title()), MARGIN, cursorY - 14f);
        cursorY -= 30f;
        String meta = "VECOPHARM - VECO ASSETS - genere le " + DATETIME_FMT.format(report.generatedAt())
                + " - " + report.rows().size() + " ligne(s)";
        drawText(stream, regular, 9f, sanitize(meta), MARGIN, cursorY - 9f);
        cursorY -= 20f;
        return cursorY;
    }

    private float drawTableHeaderRow(PDPageContentStream stream, PDFont bold, List<String> columns, float[] widths, float cursorY) throws IOException {
        stream.setNonStrokingColor(0.85f, 0.85f, 0.85f);
        stream.addRect(MARGIN, cursorY - ROW_HEIGHT, sum(widths), ROW_HEIGHT);
        stream.fill();
        stream.setNonStrokingColor(0f, 0f, 0f);

        float x = MARGIN;
        for (int c = 0; c < columns.size(); c++) {
            drawCell(stream, bold, columns.get(c), x, cursorY, widths[c]);
            x += widths[c];
        }
        return cursorY - ROW_HEIGHT;
    }

    private void drawTableRow(PDPageContentStream stream, PDFont font, List<String> row, float[] widths, float cursorY) throws IOException {
        float x = MARGIN;
        for (int c = 0; c < widths.length; c++) {
            String value = c < row.size() ? row.get(c) : "";
            drawCell(stream, font, value, x, cursorY, widths[c]);
            x += widths[c];
        }
    }

    private void drawCell(PDPageContentStream stream, PDFont font, String value, float x, float cursorY, float width) throws IOException {
        String truncated = truncateToWidth(font, FONT_SIZE, sanitize(value), width - 2 * CELL_PADDING);
        drawText(stream, font, FONT_SIZE, truncated, x + CELL_PADDING, cursorY - ROW_HEIGHT + 4.5f);
    }

    private void drawFooter(PDPageContentStream stream, PDFont font, int pageNumber, int totalPages) throws IOException {
        String text = "Page " + pageNumber + " / " + totalPages;
        drawText(stream, font, 8f, text, PAGE_WIDTH - MARGIN - font.getStringWidth(text) / 1000f * 8f, MARGIN - 12f);
    }

    private void drawText(PDPageContentStream stream, PDFont font, float size, String text, float x, float y) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    /** Meme heuristique que {@link ReportExcelExporter} (longueur de contenu), convertie en points PDF plutot qu'en "largeur de caractere Excel". */
    private float[] computeColumnWidths(ReportResultDto report, float contentWidth) {
        int columnCount = report.columns().size();
        double[] weights = new double[columnCount];
        for (int c = 0; c < columnCount; c++) {
            weights[c] = report.columns().get(c).length();
        }
        for (List<String> row : report.rows()) {
            for (int c = 0; c < columnCount && c < row.size(); c++) {
                // Cap a 35 caracteres : une colonne "Description" tres longue ne doit pas ecraser
                // les autres colonnes - le contenu reste affiche, simplement tronque a l'ecran/PDF.
                int len = row.get(c) == null ? 0 : Math.min(row.get(c).length(), 35);
                weights[c] = Math.max(weights[c], len);
            }
        }
        double totalWeight = 0;
        for (double w : weights) {
            totalWeight += Math.max(w, 4);
        }
        float[] widths = new float[columnCount];
        float minWidth = 40f;
        for (int c = 0; c < columnCount; c++) {
            float w = totalWeight == 0 ? contentWidth / columnCount : (float) (Math.max(weights[c], 4) / totalWeight * contentWidth);
            widths[c] = Math.max(w, minWidth);
        }
        // Normalise pour que la somme corresponde exactement a contentWidth (les planchers min peuvent la depasser).
        float sum = sum(widths);
        if (sum > contentWidth) {
            float scale = contentWidth / sum;
            for (int c = 0; c < columnCount; c++) {
                widths[c] *= scale;
            }
        }
        return widths;
    }

    private static float sum(float[] values) {
        float total = 0;
        for (float v : values) {
            total += v;
        }
        return total;
    }

    private String truncateToWidth(PDFont font, float size, String text, float maxWidth) throws IOException {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.getStringWidth(text) / 1000f * size <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        StringBuilder truncated = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = truncated.toString() + text.charAt(i) + ellipsis;
            if (font.getStringWidth(candidate) / 1000f * size > maxWidth) {
                break;
            }
            truncated.append(text.charAt(i));
        }
        return truncated + ellipsis;
    }

    /**
     * Meme neutralisation que {@link dz.vecopharm.vecoassets.service.LabelPdfBuilder#sanitize}
     * (polices Standard 14 Helvetica = WinAnsiEncoding uniquement) : les
     * donnees d'un rapport (designation libre, commentaires...) peuvent
     * contenir des caracteres hors de cette plage.
     */
    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sanitized.append((c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xFF) ? c : '?');
        }
        return sanitized.toString();
    }
}
