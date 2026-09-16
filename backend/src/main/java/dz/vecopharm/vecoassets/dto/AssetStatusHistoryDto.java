package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.StatusHistoryField;

import java.time.Instant;
import java.util.UUID;

public record AssetStatusHistoryDto(
        UUID id,
        StatusHistoryField fieldName,
        String oldValue,
        String newValue,
        String changedByName,
        Instant changedAt,
        String comment
) {
}
