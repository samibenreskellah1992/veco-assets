package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BuildingRequest(
        @NotNull UUID siteId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name
) {
}
