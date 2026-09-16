package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

public record BuildingDto(
        UUID id,
        UUID siteId,
        String siteName,
        String code,
        String name,
        boolean active
) {
}
