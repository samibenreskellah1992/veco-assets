package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.ZoneDto;
import dz.vecopharm.vecoassets.dto.ZoneRequest;
import dz.vecopharm.vecoassets.service.ZoneService;
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
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping
    public List<ZoneDto> findAll(@RequestParam(name = "floorId", required = false) UUID floorId) {
        return zoneService.findAll(floorId);
    }

    @GetMapping("/{id}")
    public ZoneDto findById(@PathVariable UUID id) {
        return zoneService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<ZoneDto> create(@Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ZoneDto update(@PathVariable UUID id, @Valid @RequestBody ZoneRequest request) {
        return zoneService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ZoneDto activate(@PathVariable UUID id) {
        return zoneService.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ZoneDto deactivate(@PathVariable UUID id) {
        return zoneService.setActive(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
