package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.AnomalyType;
import dz.vecopharm.vecoassets.entity.ScanResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Un scan realise pendant une campagne d'inventaire. {@code assetCode} est
 * la seule donnee portee par le QR code de l'immobilisation (jamais une URL
 * ni une donnee personnelle - prompt maitre section 13) : c'est ce code que
 * le lecteur/scanner transmet tel quel. {@code anomalyType} n'est requis que
 * lorsque {@code result == ANOMALIE} et que le code correspond a une
 * immobilisation connue (voir {@code InventoryScanService} pour le cas d'un
 * code non reconnu, qui force {@code NON_REFERENCEE} cote serveur).
 */
public record ScanRequest(
        @NotBlank @Size(max = 30) String assetCode,
        @NotNull ScanResult result,
        AnomalyType anomalyType,
        String comment
) {
}
