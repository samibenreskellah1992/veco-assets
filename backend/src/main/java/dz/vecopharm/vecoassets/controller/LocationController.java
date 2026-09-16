package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.LocationDto;
import dz.vecopharm.vecoassets.dto.LocationRequest;
import dz.vecopharm.vecoassets.service.LocationService;
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
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<LocationDto> findAll(@RequestParam(name = "zoneId", required = false) UUID zoneId) {
        return locationService.findAll(zoneId);
    }

    @GetMapping("/{id}")
    public LocationDto findById(@PathVariable UUID id) {
        return locationService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<LocationDto> create(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public LocationDto update(@PathVariable UUID id, @Valid @RequestBody LocationRequest request) {
        return locationService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public LocationDto activate(@PathVariable UUID id) {
        return locationService.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public LocationDto deactivate(@PathVariable UUID id) {
        return locationService.setActive(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
