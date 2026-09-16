package dz.vecopharm.vecoassets.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Generation reelle des images QR code / code-barres pour les etiquettes
 * (prompt maitre Phase 6), via ZXing - jamais une image statique ou un
 * placeholder graphique (regle 63 : ne jamais simuler une fonctionnalite).
 *
 * Le QR code n'encode que le code immobilisation (prompt maitre section
 * 13 : "identifie uniquement le code immobilisation") - jamais de donnees
 * personnelles ni d'URL, pour rester lisible par n'importe quel lecteur QR
 * generique sans dependance a une application VECO ASSETS.
 */
@Component
public class LabelImageGenerator {

    /** Marge minimale autour du symbole, en modules - evite un QR illisible collé au bord de l'étiquette. */
    private static final int QUIET_ZONE_MODULES = 1;

    public BufferedImage qrCode(String content, int sizePx) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, QUIET_ZONE_MODULES);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        return encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
    }

    /** Code128 : supporte directement l'alphabet du code immobilisation (lettres, chiffres, tiret). */
    public BufferedImage barcode(String content, int widthPx, int heightPx) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, QUIET_ZONE_MODULES);
        return encode(content, BarcodeFormat.CODE_128, widthPx, heightPx, hints);
    }

    private BufferedImage encode(String content, BarcodeFormat format, int width, int height, Map<EncodeHintType, Object> hints) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, format, width, height, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException ex) {
            // Ne devrait jamais arriver pour un code immobilisation genere
            // par AssetCodeGenerator (alphabet toujours valide pour QR et
            // Code128) - si ca arrive quand meme, c'est une anomalie
            // interne, pas une erreur metier a exposer proprement a l'utilisateur.
            throw new IllegalStateException("Echec de generation de l'image " + format + " pour '" + content + "'", ex);
        }
    }
}
