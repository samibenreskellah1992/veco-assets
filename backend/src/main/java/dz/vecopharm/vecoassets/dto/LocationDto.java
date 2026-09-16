package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

public record LocationDto(
        UUID id,
        UUID zoneId,
        String zoneName,
        String code,
        String name,
        boolean active
) {
}
