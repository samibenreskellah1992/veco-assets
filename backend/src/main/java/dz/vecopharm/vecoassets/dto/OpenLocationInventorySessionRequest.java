package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenLocationInventorySessionRequest(
        @NotNull UUID locationId
) {
}
