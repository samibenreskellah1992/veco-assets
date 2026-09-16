package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** {@code parentId} est nullable : une categorie racine n'a pas de parent. */
public record AssetCategoryRequest(
        UUID parentId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name
) {
}
