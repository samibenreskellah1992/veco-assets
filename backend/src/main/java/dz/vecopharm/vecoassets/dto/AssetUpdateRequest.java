package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Modification d'une immobilisation. Porte a la fois la localisation,
 * l'affectation et l'etat/statut courants : {@code AssetService} compare
 * avec l'etat precedent et ecrit l'historique correspondant
 * ({@code AssetStatusHistory} pour condition/statut,
 * {@code AssetAssignment} pour l'affectation) plutot que de laisser ces
 * changements silencieux (prompt maitre section 11/26).
 */
public record AssetUpdateRequest(
        @NotBlank @Size(max = 255) String designation,
        @NotNull UUID categoryId,
        @Size(max = 100) String brand,
        @Size(max = 100) String model,
        @Size(max = 150) String serialNumber,
        @NotNull UUID siteId,
        UUID buildingId,
        UUID floorId,
        UUID zoneId,
        UUID locationId,
        @Size(max = 150) String direction,
        @Size(max = 150) String department,
        @Size(max = 150) String service,
        UUID currentUserId,
        UUID responsibleUserId,
        LocalDate acquisitionDate,
        @Size(max = 150) String supplier,
        @Size(max = 100) String invoiceNumber,
        @DecimalMin(value = "0", inclusive = true) BigDecimal acquisitionValue,
        LocalDate commissioningDate,
        LocalDate warrantyUntil,
        @NotNull AssetCondition condition,
        @NotNull AssetStatus status,
        String comment,
        String changeComment
) {
}
