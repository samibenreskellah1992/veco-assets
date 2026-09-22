package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

/**
 * Progression d'une session de scan de local, calculee en temps reel -
 * meme discipline que {@link CampaignProgressDto} (Phase 7).
 */
public record LocationSessionProgressDto(
        UUID sessionId,
        long totalAssetsExpected,
        long scannedAssetsCount,
        long presentCount,
        long anomaliesCount,
        long remainingCount,
        double progressPercent
) {
}
