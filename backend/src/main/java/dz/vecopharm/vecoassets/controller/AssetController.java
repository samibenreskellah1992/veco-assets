package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.AssetAssignmentDto;
import dz.vecopharm.vecoassets.dto.AssetCreateRequest;
import dz.vecopharm.vecoassets.dto.AssetDto;
import dz.vecopharm.vecoassets.dto.AssetStatusHistoryDto;
import dz.vecopharm.vecoassets.dto.AssetUpdateRequest;
import dz.vecopharm.vecoassets.dto.PageResponse;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
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

import java.util.List;
import java.util.UUID;

/**
 * Immobilisations (prompt maitre Phase 5 / section 10-11). Lecture reservee
 * a IMMOBILISATION_VIEW (deja possede par tous les roles metier - voir V3),
 * ecriture repartie sur CREATE/EDIT/ARCHIVE - jamais uniquement filtre cote
 * frontend (meme convention que le referentiel en Phase 4).
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('IMMOBILISATION_VIEW')")
    public PageResponse<AssetDto> list(
            @PageableDefault(size = 20) @SortDefault(sort = "designation", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted,
            @RequestParam(name = "siteId", required = false) UUID siteId,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "locationId", required = false) UUID locationId,
            @RequestParam(name = "condition", required = false) AssetCondition condition,
            @RequestParam(name = "status", required = false) AssetStatus status,
            @RequestParam(name = "labeled", required = false) Boolean labeled,
            @RequestParam(name = "search", required = false) String search
    ) {
        return assetService.list(pageable, includeDeleted, siteId, categoryId, locationId, condition, status, labeled, search);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('IMMOBILISATION_VIEW')")
    public AssetDto findById(@PathVariable UUID id) {
        return assetService.findById(id);
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('IMMOBILISATION_VIEW')")
    public List<AssetStatusHistoryDto> statusHistory(@PathVariable UUID id) {
        return assetService.statusHistory(id);
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAuthority('IMMOBILISATION_VIEW')")
    public List<AssetAssignmentDto> assignments(@PathVariable UUID id) {
        return assetService.assignments(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('IMMOBILISATION_CREATE')")
    public ResponseEntity<AssetDto> create(@Valid @RequestBody AssetCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('IMMOBILISATION_EDIT')")
    public AssetDto update(@PathVariable UUID id, @Valid @RequestBody AssetUpdateRequest request) {
        return assetService.update(id, request);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('IMMOBILISATION_ARCHIVE')")
    public AssetDto archive(@PathVariable UUID id) {
        return assetService.archive(id);
    }
}
