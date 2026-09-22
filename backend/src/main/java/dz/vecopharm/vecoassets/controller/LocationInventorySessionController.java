package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.AssetDto;
import dz.vecopharm.vecoassets.dto.InventoryAnomalyDto;
import dz.vecopharm.vecoassets.dto.InventoryScanDto;
import dz.vecopharm.vecoassets.dto.LocationInventorySessionDto;
import dz.vecopharm.vecoassets.dto.LocationSessionProgressDto;
import dz.vecopharm.vecoassets.dto.OpenLocationInventorySessionRequest;
import dz.vecopharm.vecoassets.dto.ScanRequest;
import dz.vecopharm.vecoassets.dto.ScanResponse;
import dz.vecopharm.vecoassets.service.InventoryAnomalyService;
import dz.vecopharm.vecoassets.service.InventoryScanService;
import dz.vecopharm.vecoassets.service.LocationInventorySessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Sessions de scan d'inventaire par local (Checkpoint 3 de l'evolution
 * "locaux scannables", 2026-09). Meme repartition de permissions que
 * {@link InventoryCampaignController} (Phase 7) : ouvrir/scanner une
 * session est reserve a {@code INVENTAIRE_EXECUTE} (INVENTORISTE en fait
 * partie), tandis que la validation reste reservee a {@code
 * INVENTAIRE_VALIDATE} (GESTIONNAIRE_PATRIMOINE/ADMIN) - separation
 * conservee ici a l'identique bien que la session soit plus legere qu'une
 * campagne, pour ne pas introduire un nouveau modele de permission.
 */
@RestController
@RequestMapping("/api/location-inventory-sessions")
public class LocationInventorySessionController {

    private final LocationInventorySessionService sessionService;
    private final InventoryScanService scanService;
    private final InventoryAnomalyService anomalyService;

    public LocationInventorySessionController(
            LocationInventorySessionService sessionService,
            InventoryScanService scanService,
            InventoryAnomalyService anomalyService
    ) {
        this.sessionService = sessionService;
        this.scanService = scanService;
        this.anomalyService = anomalyService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<LocationInventorySessionDto> list(@RequestParam(name = "locationId", required = false) UUID locationId) {
        return sessionService.list(locationId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public LocationInventorySessionDto findById(@PathVariable UUID id) {
        return sessionService.findById(id);
    }

    @GetMapping("/{id}/progress")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public LocationSessionProgressDto progress(@PathVariable UUID id) {
        return sessionService.progress(id);
    }

    @GetMapping("/{id}/pending-assets")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<AssetDto> pendingAssets(@PathVariable UUID id) {
        return sessionService.pendingAssets(id);
    }

    @GetMapping("/{id}/scans")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<InventoryScanDto> scans(@PathVariable UUID id) {
        return scanService.historyForLocationSession(id);
    }

    @GetMapping("/{id}/anomalies")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public List<InventoryAnomalyDto> anomalies(@PathVariable UUID id) {
        return anomalyService.listByLocationSession(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTAIRE_EXECUTE')")
    public ResponseEntity<LocationInventorySessionDto> open(@Valid @RequestBody OpenLocationInventorySessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.open(request.locationId()));
    }

    @PostMapping("/{id}/scans")
    @PreAuthorize("hasAuthority('INVENTAIRE_EXECUTE')")
    public ScanResponse scan(@PathVariable UUID id, @Valid @RequestBody ScanRequest request) {
        return scanService.scanForLocationSession(id, request);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('INVENTAIRE_VALIDATE')")
    public LocationInventorySessionDto validate(@PathVariable UUID id) {
        return sessionService.validate(id);
    }
}
