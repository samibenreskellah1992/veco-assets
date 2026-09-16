package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.AssetDto;
import dz.vecopharm.vecoassets.dto.CampaignProgressDto;
import dz.vecopharm.vecoassets.dto.CampaignStatusUpdateRequest;
import dz.vecopharm.vecoassets.dto.InventoryAnomalyDto;
import dz.vecopharm.vecoassets.dto.InventoryCampaignDto;
import dz.vecopharm.vecoassets.dto.InventoryCampaignRequest;
import dz.vecopharm.vecoassets.dto.InventoryScanDto;
import dz.vecopharm.vecoassets.dto.ScanRequest;
import dz.vecopharm.vecoassets.dto.ScanResponse;
import dz.vecopharm.vecoassets.entity.CampaignStatus;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.service.InventoryAnomalyService;
import dz.vecopharm.vecoassets.service.InventoryCampaignService;
import dz.vecopharm.vecoassets.service.InventoryScanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Campagnes d'inventaire (prompt maitre Phase 7). Faire progresser une
 * campagne jusqu'a {@code TERMINE} est reserve a {@code INVENTAIRE_CREATE}
 * (organiser/executer la campagne), tandis que {@code VALIDE}/{@code
 * CLOTURE} exigent {@code INVENTAIRE_VALIDATE} - meme si, avec les
 * permissions seedees en Phase 2, seul GESTIONNAIRE_PATRIMOINE (et ADMIN)
 * possede les deux aujourd'hui, cette separation reste la bonne defense en
 * profondeur si les permissions d'un role venaient a evoluer.
 */
@RestController
@RequestMapping("/api/inventory-campaigns")
public class InventoryCampaignController {

    private static final EnumSet<CampaignStatus> ADVANCE_TARGETS =
            EnumSet.of(CampaignStatus.EN_PREPARATION, CampaignStatus.EN_COURS, CampaignStatus.TERMINE);

    private final InventoryCampaignService campaignService;
    private final InventoryScanService scanService;
    private final InventoryAnomalyService anomalyService;

    public InventoryCampaignController(
            InventoryCampaignService campaignService,
            InventoryScanService scanService,
            InventoryAnomalyService anomalyService
    ) {
        this.campaignService = campaignService;
        this.scanService = scanService;
        this.anomalyService = anomalyService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<InventoryCampaignDto> list(
            @RequestParam(name = "siteId", required = false) UUID siteId,
            @RequestParam(name = "status", required = false) CampaignStatus status
    ) {
        return campaignService.list(siteId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public InventoryCampaignDto findById(@PathVariable UUID id) {
        return campaignService.findById(id);
    }

    @GetMapping("/{id}/progress")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public CampaignProgressDto progress(@PathVariable UUID id) {
        return campaignService.progress(id);
    }

    @GetMapping("/{id}/pending-assets")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<AssetDto> pendingAssets(@PathVariable UUID id) {
        return campaignService.pendingAssets(id);
    }

    @GetMapping("/{id}/scans")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<InventoryScanDto> scans(@PathVariable UUID id) {
        return scanService.historyForCampaign(id);
    }

    @GetMapping("/{id}/anomalies")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<InventoryAnomalyDto> anomalies(@PathVariable UUID id) {
        return anomalyService.listByCampaign(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTAIRE_CREATE')")
    public ResponseEntity<InventoryCampaignDto> create(@Valid @RequestBody InventoryCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTAIRE_CREATE')")
    public InventoryCampaignDto update(@PathVariable UUID id, @Valid @RequestBody InventoryCampaignRequest request) {
        return campaignService.update(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('INVENTAIRE_CREATE')")
    public InventoryCampaignDto advance(@PathVariable UUID id, @Valid @RequestBody CampaignStatusUpdateRequest request) {
        if (!ADVANCE_TARGETS.contains(request.status())) {
            throw new BusinessRuleException(
                    "Cet endpoint ne fait progresser une campagne que jusqu'a TERMINE - utilisez /validate puis /close pour la suite");
        }
        return campaignService.transition(id, request.status());
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('INVENTAIRE_VALIDATE')")
    public InventoryCampaignDto validate(@PathVariable UUID id) {
        return campaignService.transition(id, CampaignStatus.VALIDE);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('INVENTAIRE_VALIDATE')")
    public InventoryCampaignDto close(@PathVariable UUID id) {
        return campaignService.transition(id, CampaignStatus.CLOTURE);
    }

    @PostMapping("/{id}/scans")
    @PreAuthorize("hasAuthority('INVENTAIRE_EXECUTE')")
    public ScanResponse scan(@PathVariable UUID id, @Valid @RequestBody ScanRequest request) {
        return scanService.scan(id, request);
    }
}
