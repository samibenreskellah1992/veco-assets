package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * Creation d'un compte par un administrateur (prompt maitre Phase 4). Le
 * mot de passe initial est fixe par l'administrateur et communique de vive
 * voix / hors application - aucune notification email n'existe en V1
 * (hors perimetre, voir docs/ROADMAP.md section 12).
 */
public record UserCreateRequest(
        @Size(max = 30) String matricule,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        UUID siteId,
        @Size(max = 150) String department,
        @Size(max = 150) String service,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotEmpty Set<String> roleCodes
) {
}
