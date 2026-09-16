package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.AnomalyStatus;
import jakarta.validation.constraints.NotNull;

public record AnomalyStatusUpdateRequest(@NotNull AnomalyStatus status) {
}
