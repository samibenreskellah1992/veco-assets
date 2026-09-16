package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Creation/modification d'une campagne d'inventaire. Le statut n'est jamais
 * fourni ici : une campagne demarre toujours {@code BROUILLON}, et toute
 * progression passe par les endpoints de transition dedies (voir
 * {@code InventoryCampaignController}), jamais par une simple modification
 * de champ.
 */
public record InventoryCampaignRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull UUID siteId,
        UUID zoneId,
        UUID responsibleUserId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
