package dz.vecopharm.vecoassets.dto;

import java.time.Instant;
import java.util.UUID;

/** Metadonnees d'une piece jointe (prompt maitre section 47) - le fichier lui-meme se recupere via {@code GET /api/attachments/{id}/download}. */
public record AttachmentDto(
        UUID id,
        String ownerType,
        UUID ownerId,
        String category,
        String fileName,
        String contentType,
        Long sizeBytes,
        UUID uploadedById,
        String uploadedByName,
        Instant createdAt
) {
}
