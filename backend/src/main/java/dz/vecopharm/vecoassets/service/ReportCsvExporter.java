package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.ReportResultDto;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Export CSV d'un rapport (prompt maitre Phase 9). Aucune dependance
 * necessaire (format texte simple) - point de comparaison volontaire avec
 * {@link ReportExcelExporter}/{@link ReportPdfExporter} qui en ont besoin.
 * Delimiteur point-virgule + BOM UTF-8 : Excel FR (locale de VECOPHARM)
 * n'interprete correctement un CSV virgule qu'avec un reglage regional
 * different de celui d'un poste francophone standard - le point-virgule
 * evite ce piege classique a l'ouverture directe du fichier.
 */
@Component
public class ReportCsvExporter {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public byte[] export(ReportResultDto report) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, report.columns());
        for (List<String> row : report.rows()) {
            appendRow(sb, row);
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(UTF8_BOM);
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Echec de generation du CSV", ex);
        }
    }

    private void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append("\r\n");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}
