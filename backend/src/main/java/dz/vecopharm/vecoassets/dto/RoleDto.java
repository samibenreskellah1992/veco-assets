package dz.vecopharm.vecoassets.dto;

import java.util.UUID;

/** Lecture seule (prompt maitre Phase 4 : les 6 roles V1 sont fixes, non administrables). */
public record RoleDto(
        UUID id,
        String code,
        String label,
        String description
) {
}
