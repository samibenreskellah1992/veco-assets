package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.AnomalyStatusUpdateRequest;
import dz.vecopharm.vecoassets.dto.AttachmentDto;
import dz.vecopharm.vecoassets.dto.InventoryAnomalyDto;
import dz.vecopharm.vecoassets.service.InventoryAnomalyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Anomalies d'inventaire (prompt maitre Phase 7 / section 16). La liste par
 * campagne vit sous {@code /api/inventory-campaigns/{id}/anomalies}
 * (InventoryCampaignController) ; ce controller porte les operations qui
 * s'appliquent a une anomalie precise, plus une vue transverse.
 */
@RestController
@RequestMapping("/api/inventory-anomalies")
public class InventoryAnomalyController {

    private final InventoryAnomalyService anomalyService;

    public InventoryAnomalyController(InventoryAnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<InventoryAnomalyDto> listAll() {
        return anomalyService.listAll();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('INVENTAIRE_VALIDATE')")
    public InventoryAnomalyDto updateStatus(@PathVariable UUID id, @Valid @RequestBody AnomalyStatusUpdateRequest request) {
        return anomalyService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/photos")
    @PreAuthorize("hasAuthority('INVENTAIRE_EXECUTE')")
    public AttachmentDto attachPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return anomalyService.attachPhoto(id, file);
    }

    @GetMapping("/{id}/photos")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<AttachmentDto> photos(@PathVariable UUID id) {
        return anomalyService.photos(id);
    }
}
