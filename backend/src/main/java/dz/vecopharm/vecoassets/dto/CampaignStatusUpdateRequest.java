package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.CampaignStatus;
import jakarta.validation.constraints.NotNull;

/** Cible d'une transition de statut de campagne (voir {@code InventoryCampaignService#transition}). */
public record CampaignStatusUpdateRequest(@NotNull CampaignStatus status) {
}
