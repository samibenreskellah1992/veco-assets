package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vue complete d'une immobilisation, avec les libelles resolus des entites
 * liees (categorie, localisation, utilisateurs) pour eviter des allers-
 * retours supplementaires cote frontend - prompt maitre section 10/11.
 */
public record AssetDto(
        UUID id,
        String assetCode,
        String designation,
        UUID categoryId,
        String categoryName,
        String brand,
        String model,
        String serialNumber,
        UUID siteId,
        String siteName,
        UUID buildingId,
        String buildingName,
        UUID floorId,
        String floorName,
        UUID zoneId,
        String zoneName,
        UUID locationId,
        String locationName,
        String direction,
        String department,
        String service,
        UUID currentUserId,
        String currentUserName,
        UUID responsibleUserId,
        String responsibleUserName,
        LocalDate acquisitionDate,
        String supplier,
        String invoiceNumber,
        BigDecimal acquisitionValue,
        LocalDate commissioningDate,
        LocalDate warrantyUntil,
        AssetCondition condition,
        AssetStatus status,
        String comment,
        boolean labeled,
        Instant lastInventoryAt,
        boolean deleted,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
