package dz.vecopharm.vecoassets.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserSummaryDto user
) {
}
