package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/** Ne porte jamais de mot de passe - voir {@link ResetPasswordRequest} pour la reinitialisation, une action distincte et explicite. */
public record UserUpdateRequest(
        @Size(max = 30) String matricule,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        UUID siteId,
        @Size(max = 150) String department,
        @Size(max = 150) String service,
        @NotNull UserStatus status,
        @NotEmpty Set<String> roleCodes
) {
}
