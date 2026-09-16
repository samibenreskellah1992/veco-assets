package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.AssetLabelDto;
import dz.vecopharm.vecoassets.dto.LabelGenerationRequest;
import dz.vecopharm.vecoassets.service.AssetLabelService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Etiquetage (prompt maitre Phase 6). Separe de {@link AssetController} car
 * ce module a ses propres permissions ({@code ETIQUETTE_GENERATE}) et
 * produit un flux binaire (PDF) plutot qu'un DTO JSON.
 */
@RestController
public class LabelController {

    private final AssetLabelService assetLabelService;

    public LabelController(AssetLabelService assetLabelService) {
        this.assetLabelService = assetLabelService;
    }

    /**
     * Genere un PDF (une page par immobilisation, aux dimensions reelles du
     * format choisi) et le renvoie directement dans la reponse - le
     * frontend l'utilise aussi bien pour une previsualisation (affichage
     * inline du PDF reel) que pour le telechargement, sans endpoint separe.
     */
    @PostMapping("/api/labels/generate")
    @PreAuthorize("hasAuthority('ETIQUETTE_GENERATE')")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody LabelGenerationRequest request) {
        byte[] pdf = assetLabelService.generate(request.assetIds(), request.formatId());
        ContentDisposition disposition = ContentDisposition.inline().filename("etiquettes.pdf").build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }

    @GetMapping("/api/assets/{id}/labels")
    @PreAuthorize("hasAuthority('IMMOBILISATION_VIEW')")
    public List<AssetLabelDto> history(@PathVariable UUID id) {
        return assetLabelService.historyForAsset(id);
    }
}
