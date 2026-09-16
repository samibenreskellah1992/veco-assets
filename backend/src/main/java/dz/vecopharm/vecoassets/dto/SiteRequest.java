package dz.vecopharm.vecoassets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Creation et modification (le statut actif/inactif se gere via des endpoints dedies, pas ici). */
public record SiteRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String address,
        @Size(max = 100) String city
) {
}
