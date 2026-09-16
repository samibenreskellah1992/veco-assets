package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

public record ZoneDto(
        UUID id,
        UUID floorId,
        String floorName,
        String code,
        String name,
        boolean active
) {
}
