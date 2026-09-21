package dz.vecopharm.vecoassets.dto;

import java.time.Instant;
import java.util.UUID;

/** Checkpoint 2 "locaux scannables" (2026-09) - mirroir de {@link AssetLabelDto}. */
public record LocationLabelDto(
        UUID id,
        UUID formatId,
        String formatCode,
        String formatName,
        String generatedByName,
        Instant generatedAt
) {
}
