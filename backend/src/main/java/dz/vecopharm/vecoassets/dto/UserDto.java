package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vue Administration > Utilisateurs (Phase 4) - distincte de
 * {@link UserSummaryDto} (Phase 3, profil "qui suis-je" apres login) :
 * celle-ci expose les identifiants necessaires a un formulaire d'edition
 * (siteId, statut, codes de role) plutot que des libelles d'affichage
 * seuls. Ne contient jamais {@code passwordHash}.
 */
public record UserDto(
        UUID id,
        String matricule,
        String firstName,
        String lastName,
        String email,
        String phone,
        UUID siteId,
        String siteName,
        String department,
        String service,
        UserStatus status,
        Instant lastLoginAt,
        List<String> roleCodes
) {
}
