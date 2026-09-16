package dz.vecopharm.vecoassets.dto;

/**
 * Resultat d'un scan. {@code assetRecognized} distingue le cas ou le code
 * scanne ne correspond a aucune immobilisation connue - dans ce cas {@code
 * scan} est {@code null} (impossible de rattacher un {@code inventory_scans}
 * a une immobilisation inexistante, contrainte {@code NOT NULL} en base) et
 * seul {@code anomaly} (type {@code NON_REFERENCEE}) est renvoye.
 */
public record ScanResponse(
        boolean assetRecognized,
        InventoryScanDto scan,
        InventoryAnomalyDto anomaly
) {
}
