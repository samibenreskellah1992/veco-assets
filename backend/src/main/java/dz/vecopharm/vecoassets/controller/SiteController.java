package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.SiteDto;
import dz.vecopharm.vecoassets.dto.SiteRequest;
import dz.vecopharm.vecoassets.service.SiteService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Referentiel > Sites (prompt maitre Phase 4). Lecture ouverte a tout
 * utilisateur authentifie (les autres modules en auront besoin dans les
 * listes deroulantes), ecriture reservee a REFERENTIEL_MANAGE - jamais
 * uniquement filtre cote frontend.
 */
@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    public List<SiteDto> findAll() {
        return siteService.findAll();
    }

    @GetMapping("/{id}")
    public SiteDto findById(@PathVariable UUID id) {
        return siteService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<SiteDto> create(@Valid @RequestBody SiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public SiteDto update(@PathVariable UUID id, @Valid @RequestBody SiteRequest request) {
        return siteService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public SiteDto activate(@PathVariable UUID id) {
        return siteService.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public SiteDto deactivate(@PathVariable UUID id) {
        return siteService.setActive(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REFERENTIEL_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        siteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
