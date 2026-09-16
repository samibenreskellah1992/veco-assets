package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

public record FloorDto(
        UUID id,
        UUID buildingId,
        String buildingName,
        String code,
        String name,
        boolean active
) {
}
