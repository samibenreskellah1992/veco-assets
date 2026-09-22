package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.ScanResult;

import java.time.Instant;
import java.util.UUID;

public record InventoryScanDto(
        UUID id,
        UUID campaignId,
        String campaignName,
        UUID locationSessionId,
        UUID assetId,
        String assetCode,
        String assetDesignation,
        UUID scannedById,
        String scannedByName,
        Instant scannedAt,
        ScanResult result,
        String comment
) {
}
