package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.AssetCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Creation d'une immobilisation. Le code (asset_code) n'est jamais saisi
 * par l'utilisateur : il est genere automatiquement par
 * {@code AssetCodeGenerator} a partir du parametre {@code asset_code.format}
 * (prompt maitre section 53). Le statut et l'etat de depart sont
 * volontairement absents ici : une nouvelle immobilisation demarre
 * toujours {@code NEUF}/{@code EN_STOCK}, coherent avec les valeurs par
 * defaut de l'entite - tout changement immediat passe par l'endpoint de
 * modification, qui trace l'historique.
 */
public record AssetCreateRequest(
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
        AssetCondition condition,
        String comment
) {
}
