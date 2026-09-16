package dz.vecopharm.vecoassets.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        String userFullName,
        String action,
        String module,
        String entityName,
        UUID entityId,
        String oldValue,
        String newValue,
        String ipAddress,
        Instant occurredAt
) {
}
