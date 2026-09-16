package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.BuildingDto;
import dz.vecopharm.vecoassets.dto.BuildingRequest;
import dz.vecopharm.vecoassets.service.BuildingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public List<BuildingDto> findAll(@RequestParam(name = "siteId", required = false) UUID siteId) {
        return buildingService.findAll(siteId);
    }

    @GetMapping("/{id}")
    public BuildingDto findById(@PathVariable UUID id) {
        return buildingService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<BuildingDto> create(@Valid @RequestBody BuildingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(buildingService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public BuildingDto update(@PathVariable UUID id, @Valid @RequestBody BuildingRequest request) {
        return buildingService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public BuildingDto activate(@PathVariable UUID id) {
        return buildingService.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public BuildingDto deactivate(@PathVariable UUID id) {
        return buildingService.setActive(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        buildingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
