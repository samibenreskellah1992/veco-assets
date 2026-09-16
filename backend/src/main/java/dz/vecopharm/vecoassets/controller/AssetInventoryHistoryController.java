package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.InventoryScanDto;
import dz.vecopharm.vecoassets.service.InventoryScanService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Historique de scan d'une immobilisation, toutes campagnes confondues
 * (prompt maitre Phase 7). Controller separe de {@link AssetController}
 * (Phase 5), meme convention que {@code LabelController} (Phase 6) pour
 * {@code /api/assets/{id}/labels} : un module ajoute une route sous
 * {@code /api/assets/{id}/...} sans toucher au controller d'une phase
 * anterieure. Alimente l'onglet "Inventaire" de la fiche immobilisation,
 * un placeholder honnete jusqu'a cette phase.
 */
@RestController
public class AssetInventoryHistoryController {

    private final InventoryScanService inventoryScanService;

    public AssetInventoryHistoryController(InventoryScanService inventoryScanService) {
        this.inventoryScanService = inventoryScanService;
    }

    @GetMapping("/api/assets/{id}/inventory-scans")
    @PreAuthorize("hasAuthority('IMMOBILISATION_VIEW')")
    public List<InventoryScanDto> history(@PathVariable UUID id) {
        return inventoryScanService.historyForAsset(id);
    }
}
