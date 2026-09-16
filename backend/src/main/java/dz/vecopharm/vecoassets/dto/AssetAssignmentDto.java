package dz.vecopharm.vecoassets.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetAssignmentDto(
        UUID id,
        UUID userId,
        String userName,
        String direction,
        String department,
        String service,
        Instant assignedFrom,
        Instant assignedUntil,
        String assignedByName,
        String comment
) {
}
