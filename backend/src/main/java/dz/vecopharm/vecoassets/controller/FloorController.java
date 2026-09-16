package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.FloorDto;
import dz.vecopharm.vecoassets.dto.FloorRequest;
import dz.vecopharm.vecoassets.service.FloorService;
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
@RequestMapping("/api/floors")
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @GetMapping
    public List<FloorDto> findAll(@RequestParam(name = "buildingId", required = false) UUID buildingId) {
        return floorService.findAll(buildingId);
    }

    @GetMapping("/{id}")
    public FloorDto findById(@PathVariable UUID id) {
        return floorService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<FloorDto> create(@Valid @RequestBody FloorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(floorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public FloorDto update(@PathVariable UUID id, @Valid @RequestBody FloorRequest request) {
        return floorService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public FloorDto activate(@PathVariable UUID id) {
        return floorService.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public FloorDto deactivate(@PathVariable UUID id) {
        return floorService.setActive(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        floorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
