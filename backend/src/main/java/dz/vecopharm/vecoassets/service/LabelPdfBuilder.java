package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.entity.AssetLabelFormat;
import dz.vecopharm.vecoassets.entity.LabelPrintable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Construit le PDF d'etiquettes (prompt maitre Phase 6) : une page par
 * objet imprimable selectionne, aux dimensions REELLES du format choisi
 * (jamais une taille A4 generique decoupee arbitrairement) - le format
 * pilote a la fois la taille de page et ce qui est affiche (logo,
 * designation courte, QR code, code-barres), jamais code en dur ici.
 *
 * <p>Checkpoint 2 de l'evolution "locaux scannables" (2026-09) : ce
 * builder ne connait que {@link LabelPrintable} (Asset et Location
 * l'implementent tous les deux) - la mise en page QR/texte est identique
 * pour une immobilisation et un local, pas de raison de la dupliquer.</p>
 */
@Component
public class LabelPdfBuilder {

    /** 1 mm = 72/25.4 pt (unite native PDF). */
    private static final float MM_TO_PT = 72f / 25.4f;
    private static final float MARGIN_MM = 2f;
    /** Densite d'echantillonnage des images QR/code-barres embarquees (px par pt PDF) - suffisante pour une impression nette a l'echelle d'une etiquette. */
    private static final float IMAGE_PX_PER_PT = 4f;

    private final LabelImageGenerator imageGenerator;

    public LabelPdfBuilder(LabelImageGenerator imageGenerator) {
        this.imageGenerator = imageGenerator;
    }

    public byte[] build(List<? extends LabelPrintable> items, AssetLabelFormat format) {
        try (PDDocument document = new PDDocument()) {
            float widthPt = toPt(format.getWidthMm());
            float heightPt = toPt(format.getHeightMm());
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (LabelPrintable item : items) {
                PDPage page = new PDPage(new PDRectangle(widthPt, heightPt));
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    renderLabel(document, stream, item, format, widthPt, heightPt, bold, regular);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Echec de generation du PDF d'etiquettes", ex);
        }
    }

    private void renderLabel(
            PDDocument document,
            PDPageContentStream stream,
            LabelPrintable item,
            AssetLabelFormat format,
            float widthPt,
            float heightPt,
            PDFont bold,
            PDFont regular
    ) throws IOException {
        float margin = MARGIN_MM * MM_TO_PT;
        float contentWidth = widthPt - 2 * margin;
        float cursorY = heightPt - margin;

        // Simple repere de decoupe - pas un vrai trait de coupe imprimante professionnel.
        stream.setLineWidth(0.5f);
        stream.addRect(margin / 2, margin / 2, widthPt - margin, heightPt - margin);
        stream.stroke();

        if (format.isShowLogo()) {
            float fontSize = clamp(heightPt * 0.11f, 5f, 9f);
            cursorY -= fontSize;
            drawCenteredText(stream, bold, fontSize, "VECOPHARM", widthPt / 2, cursorY);
            cursorY -= fontSize * 0.4f;
        }

        if (format.isShowShortDesignation()) {
            float fontSize = clamp(heightPt * 0.09f, 4.5f, 8f);
            cursorY -= fontSize;
            String designation = truncateToWidth(regular, fontSize, sanitize(item.getLabelDesignation()), contentWidth);
            drawCenteredText(stream, regular, fontSize, designation, widthPt / 2, cursorY);
            cursorY -= fontSize * 0.5f;
        }

        float codeFontSize = clamp(heightPt * 0.09f, 4.5f, 8f);
        float reservedForCode = codeFontSize + margin * 0.5f;
        float symbolsAreaHeight = Math.max(cursorY - margin - reservedForCode, 0);
        String printedCode = sanitize(item.getLabelCode());

        if (format.isShowQrCode() && format.isShowBarcode()) {
            float qrSize = Math.min(symbolsAreaHeight * 0.62f, contentWidth * 0.62f);
            drawQrCode(document, stream, printedCode, widthPt / 2 - qrSize / 2, cursorY - qrSize, qrSize);
            float barcodeHeight = symbolsAreaHeight * 0.28f;
            float barcodeY = cursorY - symbolsAreaHeight;
            drawBarcode(document, stream, printedCode, margin, barcodeY, contentWidth, barcodeHeight);
        } else if (format.isShowQrCode()) {
            float qrSize = Math.min(symbolsAreaHeight * 0.9f, contentWidth * 0.9f);
            float qrY = cursorY - symbolsAreaHeight / 2 - qrSize / 2;
            drawQrCode(document, stream, printedCode, widthPt / 2 - qrSize / 2, qrY, qrSize);
        } else if (format.isShowBarcode()) {
            float barcodeHeight = symbolsAreaHeight * 0.55f;
            float barcodeY = cursorY - symbolsAreaHeight / 2 - barcodeHeight / 2;
            drawBarcode(document, stream, printedCode, margin, barcodeY, contentWidth, barcodeHeight);
        }

        drawCenteredText(stream, regular, codeFontSize, printedCode, widthPt / 2, margin + codeFontSize * 0.2f);
    }

    private void drawQrCode(PDDocument document, PDPageContentStream stream, String content, float x, float y, float sizePt) throws IOException {
        if (sizePt <= 0) {
            return;
        }
        int pixelSize = Math.round(sizePt * IMAGE_PX_PER_PT);
        BufferedImage image = imageGenerator.qrCode(content, pixelSize);
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
        stream.drawImage(pdImage, x, y, sizePt, sizePt);
    }

    private void drawBarcode(PDDocument document, PDPageContentStream stream, String content, float x, float y, float widthPt, float heightPt) throws IOException {
        if (widthPt <= 0 || heightPt <= 0) {
            return;
        }
        int pixelWidth = Math.round(widthPt * IMAGE_PX_PER_PT);
        int pixelHeight = Math.round(heightPt * IMAGE_PX_PER_PT);
        BufferedImage image = imageGenerator.barcode(content, pixelWidth, pixelHeight);
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
        stream.drawImage(pdImage, x, y, widthPt, heightPt);
    }

    private void drawCenteredText(PDPageContentStream stream, PDFont font, float size, String text, float centerX, float baselineY) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000f * size;
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(centerX - textWidth / 2, baselineY);
        stream.showText(text);
        stream.endText();
    }

    private String truncateToWidth(PDFont font, float size, String text, float maxWidth) throws IOException {
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
     * Les polices Standard 14 (Helvetica) ne couvrent que WinAnsiEncoding :
     * on neutralise ici tout caractere hors de cette plage (emoji, CJK, ...)
     * plutot que de laisser PDFBox lever une exception au rendu - la
     * designation d'une immobilisation est une saisie libre (Phase 5).
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

    private static float toPt(BigDecimal mm) {
        return mm.floatValue() * MM_TO_PT;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
