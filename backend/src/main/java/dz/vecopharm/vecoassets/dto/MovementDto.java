package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.MovementStatus;
import dz.vecopharm.vecoassets.entity.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementDto(
        UUID id,
        UUID assetId,
        String assetCode,
        String assetDesignation,
        MovementType movementType,
        UUID fromSiteId,
        String fromSiteName,
        UUID toSiteId,
        String toSiteName,
        UUID fromLocationId,
        String fromLocationName,
        UUID toLocationId,
        String toLocationName,
        UUID fromUserId,
        String fromUserName,
        UUID toUserId,
        String toUserName,
        String fromDirection,
        String toDirection,
        String fromDepartment,
        String toDepartment,
        String fromService,
        String toService,
        UUID requestedById,
        String requestedByName,
        UUID validatedById,
        String validatedByName,
        MovementStatus status,
        String reason,
        String comment,
        Instant requestedAt,
        Instant validatedAt,
        Instant executedAt
) {
}
