package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.service.AttachmentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Telechargement d'une piece jointe par id (prompt maitre section 47).
 * Permission volontairement fixee a {@code INVENTAIRE_VIEW} pour l'instant :
 * seules les photos d'anomalie (Phase 7, owner_type=ANOMALY) existent a ce
 * stade. A elargir (permission dependant du type de proprietaire reel) le
 * jour ou les photos d'immobilisation / PV de mouvement utiliseront aussi
 * cette table, plutot que d'anticiper une regle qui n'a pas encore de cas
 * d'usage reel.
 */
@RestController
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/api/attachments/{id}/download")
    @PreAuthorize("hasAuthority('INVENTAIRE_VIEW')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        AttachmentService.DownloadableFile file = attachmentService.download(id);
        ContentDisposition disposition = ContentDisposition.inline().filename(file.fileName()).build();
        MediaType mediaType = file.contentType() != null ? MediaType.parseMediaType(file.contentType()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                // Phase 10 (revue securite) : le contenu servi ici est deja
                // valide a l'upload (InventoryAnomalyService.attachPhoto -
                // liste blanche de types raster), mais nosniff est une
                // defense en profondeur peu couteuse contre toute tentative
                // du navigateur de reinterpreter le corps de la reponse
                // selon un type different du Content-Type declare.
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.content());
    }
}
