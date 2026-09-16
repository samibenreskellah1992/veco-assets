package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

public record AssetCategoryDto(
        UUID id,
        UUID parentId,
        String parentName,
        String code,
        String name,
        boolean active
) {
}
