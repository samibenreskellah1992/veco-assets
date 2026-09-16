package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.Size;

public record MovementRejectRequest(
        @Size(max = 2000) String comment
) {
}
