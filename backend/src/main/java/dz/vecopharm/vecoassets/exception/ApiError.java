package dz.vecopharm.vecoassets.exception;

import java.time.Instant;

/**
 * Standardized API error payload returned by {@link GlobalExceptionHandler}.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
