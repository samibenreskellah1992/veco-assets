package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.LocationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * {@code status}/{@code description}/{@code responsibleUserId} sont
 * optionnels : {@code status} defaut a {@link LocationStatus#ACTIF} en
 * service si absent, {@code qrCode} n'est volontairement pas ici (genere a
 * la creation, jamais fourni ni modifiable par le client).
 */
public record LocationRequest(
        @NotNull UUID zoneId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        LocationStatus status,
        String description,
        UUID responsibleUserId
) {
}
