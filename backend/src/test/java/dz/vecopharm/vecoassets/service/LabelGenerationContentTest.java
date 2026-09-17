package dz.vecopharm.vecoassets.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetLabelFormat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification "de bout en bout" de la generation reelle des etiquettes
 * (Phase 6) : QR code et code-barres ZXing, PDF PDFBox aux dimensions du
 * format. Contrairement a AssetCodeGeneratorTest (logique metier mockee),
 * ce test n'utilise aucun mock : il genere une vraie image, un vrai PDF,
 * l'ouvre reellement et en extrait l'image embarquee pour la redecoder -
 * afin de verifier que ce qu'un lecteur QR/code-barres generique lirait sur
 * une etiquette imprimee correspond exactement au code immobilisation
 * attendu. C'etait, avec l'export Excel (voir ReportExcelExporterContentTest),
 * le seul point du backend qui n'avait jamais ete verifie que par revue de
 * code plutot que par execution reelle (voir docs/ROADMAP.md section 13).
 *
 * Les fichiers generes sont aussi ecrits dans target/verification-output/
 * pour une inspection visuelle manuelle (PDF ouvrable dans n'importe quel
 * lecteur PDF, PNG ouvrable dans n'importe quelle visionneuse d'image).
 */
class LabelGenerationContentTest {

    private static final String ASSET_CODE = "VEC-2026-00042";

    private final LabelImageGenerator imageGenerator = new LabelImageGenerator();
    private final LabelPdfBuilder pdfBuilder = new LabelPdfBuilder(imageGenerator);

    @Test
    void qrCodeImageDecodesToExactAssetCode() throws Exception {
        BufferedImage qr = imageGenerator.qrCode(ASSET_CODE, 300);

        assertThat(qr).isNotNull();
        assertThat(decode(qr)).isEqualTo(ASSET_CODE);
    }

    @Test
    void barcodeImageDecodesToExactAssetCode() throws Exception {
        BufferedImage barcode = imageGenerator.barcode(ASSET_CODE, 400, 120);

        assertThat(barcode).isNotNull();
        assertThat(decode(barcode)).isEqualTo(ASSET_CODE);
    }

    @Test
    void pdfHasOnePagePerAssetAtFormatDimensions() throws IOException {
        AssetLabelFormat format = format(50, 30, true, true, true, false);
        List<Asset> assets = List.of(
                asset("VEC-2026-00001", "Ordinateur portable"),
                asset("VEC-2026-00002", "Ecran 24 pouces"),
                asset("VEC-2026-00003", "Imprimante laser")
        );

        byte[] pdfBytes = pdfBuilder.build(assets, format);
        writeVerificationFile("etiquettes-3-assets.pdf", pdfBytes);

        assertThat(new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);

            float expectedWidthPt = 50 * 72f / 25.4f;
            float expectedHeightPt = 30 * 72f / 25.4f;
            var mediaBox = document.getPage(0).getMediaBox();
            assertThat(mediaBox.getWidth()).isCloseTo(expectedWidthPt, Offset.offset(0.5f));
            assertThat(mediaBox.getHeight()).isCloseTo(expectedHeightPt, Offset.offset(0.5f));
        }
    }

    @Test
    void pdfEmbeddedQrImageDecodesToExactAssetCode() throws Exception {
        AssetLabelFormat format = format(40, 30, true, false, true, false);
        Asset asset = asset(ASSET_CODE, "Materiel de test");

        byte[] pdfBytes = pdfBuilder.build(List.of(asset), format);
        writeVerificationFile("etiquette-qr-seul.pdf", pdfBytes);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            BufferedImage embeddedQr = extractSingleEmbeddedImage(document.getPage(0));
            writeVerificationImage("etiquette-qr-seul-image-embarquee.png", embeddedQr);

            assertThat(decode(embeddedQr)).isEqualTo(ASSET_CODE);
        }
    }

    @Test
    void pdfEmbeddedBarcodeImageDecodesToExactAssetCode() throws Exception {
        AssetLabelFormat format = format(60, 30, false, false, false, true);
        Asset asset = asset(ASSET_CODE, "Materiel de test");

        byte[] pdfBytes = pdfBuilder.build(List.of(asset), format);
        writeVerificationFile("etiquette-barcode-seul.pdf", pdfBytes);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            BufferedImage embeddedBarcode = extractSingleEmbeddedImage(document.getPage(0));
            writeVerificationImage("etiquette-barcode-seul-image-embarquee.png", embeddedBarcode);

            assertThat(decode(embeddedBarcode)).isEqualTo(ASSET_CODE);
        }
    }

    private static BufferedImage extractSingleEmbeddedImage(PDPage page) throws IOException {
        PDResources resources = page.getResources();
        List<BufferedImage> images = new ArrayList<>();
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (xObject instanceof PDImageXObject imageXObject) {
                images.add(imageXObject.getImage());
            }
        }
        assertThat(images).as("nombre d'images embarquees dans la page PDF").hasSize(1);
        return images.get(0);
    }

    private static String decode(BufferedImage image) throws Exception {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    private static AssetLabelFormat format(int widthMm, int heightMm, boolean logo, boolean designation, boolean qr, boolean barcode) {
        AssetLabelFormat format = new AssetLabelFormat();
        format.setCode("TEST");
        format.setName("Format de test");
        format.setWidthMm(BigDecimal.valueOf(widthMm));
        format.setHeightMm(BigDecimal.valueOf(heightMm));
        format.setShowLogo(logo);
        format.setShowShortDesignation(designation);
        format.setShowQrCode(qr);
        format.setShowBarcode(barcode);
        format.setActive(true);
        return format;
    }

    private static Asset asset(String code, String designation) {
        Asset asset = new Asset();
        asset.setAssetCode(code);
        asset.setDesignation(designation);
        return asset;
    }

    private static void writeVerificationFile(String filename, byte[] content) throws IOException {
        Path dir = Path.of("target", "verification-output");
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), content);
    }

    private static void writeVerificationImage(String filename, BufferedImage image) throws IOException {
        Path dir = Path.of("target", "verification-output");
        Files.createDirectories(dir);
        ImageIO.write(image, "png", dir.resolve(filename).toFile());
    }
}
