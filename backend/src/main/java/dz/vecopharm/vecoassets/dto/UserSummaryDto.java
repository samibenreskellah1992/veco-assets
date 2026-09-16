package dz.vecopharm.vecoassets.dto;

import java.util.List;
import java.util.UUID;

/** Profil utilisateur renvoye par /api/auth/login et /api/auth/me - jamais le password_hash. */
public record UserSummaryDto(
        UUID id,
        String matricule,
        String fullName,
        String email,
        String siteName,
        String department,
        String service,
        List<String> roles,
        List<String> permissions
) {
}
