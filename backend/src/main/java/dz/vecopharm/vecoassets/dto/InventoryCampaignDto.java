package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.CampaignStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Campagne d'inventaire (prompt maitre Phase 7 / sections 14-16). Le
 * perimetre (site, zone optionnelle) determine quelles immobilisations sont
 * attendues au scan - voir {@code InventoryCampaignService#pendingAssets}.
 */
public record InventoryCampaignDto(
        UUID id,
        String name,
        UUID siteId,
        String siteName,
        UUID zoneId,
        String zoneName,
        UUID responsibleUserId,
        String responsibleUserName,
        LocalDate startDate,
        LocalDate endDate,
        CampaignStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
