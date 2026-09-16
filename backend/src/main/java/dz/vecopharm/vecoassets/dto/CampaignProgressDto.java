package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

/**
 * Progression d'une campagne calculee en temps reel a partir des donnees
 * reelles (jamais un compteur stocke qui pourrait diverger) - prompt maitre
 * Phase 7, critere de verification "progression de campagne mise a jour en
 * base reelle". {@code presentCount} compte les immobilisations avec au
 * moins un scan {@code PRESENT} dans cette campagne (une simplification
 * assumee : une immobilisation d'abord scannee en anomalie puis rescannee
 * presente compte comme presente, ce qui reflete l'etat le plus favorable
 * connu plutot que le dernier scan brut).
 */
public record CampaignProgressDto(
        UUID campaignId,
        long totalAssetsInScope,
        long scannedAssetsCount,
        long presentCount,
        long anomaliesCount,
        long remainingCount,
        double progressPercent
) {
}
