package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Creation et modification d'un format d'etiquette (prompt maitre section
 * 13 : dimensions et contenu affiche administrables, jamais codes en dur).
 * Le statut actif/inactif se gere via des endpoints dedies, comme pour le
 * referentiel (Phase 4).
 */
public record AssetLabelFormatRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin(value = "1", message = "La largeur doit etre superieure a 0") BigDecimal widthMm,
        @NotNull @DecimalMin(value = "1", message = "La hauteur doit etre superieure a 0") BigDecimal heightMm,
        boolean showLogo,
        boolean showShortDesignation,
        boolean showQrCode,
        boolean showBarcode
) {
}
