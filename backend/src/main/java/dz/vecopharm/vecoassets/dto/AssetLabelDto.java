package dz.vecopharm.vecoassets.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetLabelDto(
        UUID id,
        UUID formatId,
        String formatCode,
        String formatName,
        String generatedByName,
        Instant generatedAt
) {
}
