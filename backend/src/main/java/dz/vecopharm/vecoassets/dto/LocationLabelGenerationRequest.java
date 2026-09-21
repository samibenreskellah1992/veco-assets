package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Checkpoint 2 "locaux scannables" (2026-09) - mirroir de {@link LabelGenerationRequest}. */
public record LocationLabelGenerationRequest(
        @NotEmpty(message = "Selectionnez au moins un local") List<UUID> locationIds,
        @NotNull(message = "Format d'etiquette requis") UUID formatId
) {
}
