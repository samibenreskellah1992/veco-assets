package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

public record SiteDto(
        UUID id,
        String code,
        String name,
        String address,
        String city,
        boolean active
) {
}
