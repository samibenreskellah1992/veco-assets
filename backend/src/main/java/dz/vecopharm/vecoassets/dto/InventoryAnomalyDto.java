package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.AnomalyStatus;
import dz.vecopharm.vecoassets.entity.AnomalyType;

import java.time.Instant;
import java.util.UUID;

public record InventoryAnomalyDto(
        UUID id,
        UUID campaignId,
        String campaignName,
        UUID locationSessionId,
        UUID assetId,
        String assetCode,
        String assetDesignation,
        UUID scanId,
        AnomalyType anomalyType,
        String description,
        UUID reportedById,
        String reportedByName,
        AnomalyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
