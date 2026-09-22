package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.LocationSessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Session de scan d'inventaire pour UN local (Checkpoint 3 "locaux
 * scannables", 2026-09) - voir {@code LocationInventorySession}.
 */
public record LocationInventorySessionDto(
        UUID id,
        UUID locationId,
        String locationCode,
        String locationName,
        UUID openedById,
        String openedByName,
        Instant openedAt,
        LocationSessionStatus status,
        UUID validatedById,
        String validatedByName,
        Instant validatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
