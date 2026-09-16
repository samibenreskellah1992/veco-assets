package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.ReportResultDto;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Export Excel (.xlsx) d'un rapport (prompt maitre Phase 9), via Apache POI
 * (seule nouvelle dependance de cette phase - voir pom.xml). Les largeurs
 * de colonnes sont calculees a partir de la longueur du contenu
 * (caracteres) plutot que via {@code Sheet.autoSizeColumn} : cette methode
 * POI mesure les polices via AWT/Java2D, ce qui peut echouer ou produire
 * des largeurs incorrectes sur un serveur headless sans polices systeme
 * installees (piege classique de deploiement) - le calcul manuel ci-dessous
 * evite totalement cette dependance a AWT.
 */
@Component
public class ReportExcelExporter {

    private static final int MIN_WIDTH_CHARS = 10;
    private static final int MAX_WIDTH_CHARS = 60;

    public byte[] export(ReportResultDto report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sanitizeSheetName(report.title()));

            CellStyle titleStyle = titleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue(report.title());
            titleRow.getCell(0).setCellStyle(titleStyle);
            if (!report.columns().isEmpty()) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, report.columns().size() - 1));
            }

            Row metaRow = sheet.createRow(rowIndex++);
            metaRow.createCell(0).setCellValue("Genere le " + formatInstant(report.generatedAt()) + " - " + report.rows().size() + " ligne(s)");

            rowIndex++; // ligne vide

            Row headerRow = sheet.createRow(rowIndex++);
            for (int c = 0; c < report.columns().size(); c++) {
                var cell = headerRow.createCell(c);
                cell.setCellValue(report.columns().get(c));
                cell.setCellStyle(headerStyle);
            }

            for (List<String> dataRow : report.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int c = 0; c < dataRow.size(); c++) {
                    row.createCell(c).setCellValue(dataRow.get(c));
                }
            }

            int[] widths = computeColumnWidths(report);
            for (int c = 0; c < widths.length; c++) {
                sheet.setColumnWidth(c, widths[c] * 256);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Echec de generation du fichier Excel", ex);
        }
    }

    private int[] computeColumnWidths(ReportResultDto report) {
        int columnCount = report.columns().size();
        int[] widths = new int[columnCount];
        for (int c = 0; c < columnCount; c++) {
            widths[c] = report.columns().get(c).length();
        }
        for (List<String> row : report.rows()) {
            for (int c = 0; c < columnCount && c < row.size(); c++) {
                widths[c] = Math.max(widths[c], row.get(c) == null ? 0 : row.get(c).length());
            }
        }
        for (int c = 0; c < columnCount; c++) {
            widths[c] = Math.min(Math.max(widths[c] + 2, MIN_WIDTH_CHARS), MAX_WIDTH_CHARS);
        }
        return widths;
    }

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    /** Un nom d'onglet Excel est limite a 31 caracteres et interdit certains symboles. */
    private String sanitizeSheetName(String title) {
        String cleaned = title.replaceAll("[\\\\/*?\\[\\]:]", " ").trim();
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private String formatInstant(java.time.Instant instant) {
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.of("Africa/Algiers"))
                .format(instant);
    }
}
