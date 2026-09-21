package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.LocationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Checkpoint 1 de l'evolution "locaux scannables" (2026-09) : en plus des
 * champs deja exposes par le CRUD referentiel (zone/code/name/active), la
 * fiche locale expose desormais le statut operationnel, le code QR
 * (immutable), le responsable, le dernier inventaire, le nombre
 * d'immobilisations rattachees et toute la chaine hierarchique resolue
 * (site/batiment/etage) - dans le meme esprit que {@link AssetDto} pour la
 * hierarchie de localisation d'une immobilisation.
 */
public record LocationDto(
        UUID id,
        UUID zoneId,
        String zoneName,
        String code,
        String name,
        boolean active,
        LocationStatus status,
        String qrCode,
        String description,
        UUID responsibleUserId,
        String responsibleUserName,
        Instant lastInventoryAt,
        long assetCount,
        UUID siteId,
        String siteName,
        UUID buildingId,
        String buildingName,
        UUID floorId,
        String floorName
) {
}
