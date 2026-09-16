package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.AssetLabelFormatDto;
import dz.vecopharm.vecoassets.dto.AssetLabelFormatRequest;
import dz.vecopharm.vecoassets.service.AssetLabelFormatService;
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
 * Formats d'etiquette (prompt maitre Phase 6). Lecture ouverte a tout
 * utilisateur authentifie (necessaire au module /etiquetage pour peupler
 * le selecteur de format), ecriture reservee a ETIQUETTE_MANAGE - meme
 * convention que le referentiel en Phase 4.
 */
@RestController
@RequestMapping("/api/asset-label-formats")
public class AssetLabelFormatController {

    private final AssetLabelFormatService formatService;

    public AssetLabelFormatController(AssetLabelFormatService formatService) {
        this.formatService = formatService;
    }

    @GetMapping
    public List<AssetLabelFormatDto> findAll() {
        return formatService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ETIQUETTE_MANAGE')")
    public ResponseEntity<AssetLabelFormatDto> create(@Valid @RequestBody AssetLabelFormatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formatService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ETIQUETTE_MANAGE')")
    public AssetLabelFormatDto update(@PathVariable UUID id, @Valid @RequestBody AssetLabelFormatRequest request) {
        return formatService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ETIQUETTE_MANAGE')")
    public AssetLabelFormatDto activate(@PathVariable UUID id) {
        return formatService.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ETIQUETTE_MANAGE')")
    public AssetLabelFormatDto deactivate(@PathVariable UUID id) {
        return formatService.setActive(id, false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ETIQUETTE_MANAGE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        formatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
