package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Demande de mouvement (premiere etape du workflow Demande -> Validation ->
 * Execution -> Historisation, prompt maitre section 18). L'etat "avant"
 * (site/local/utilisateur/direction/departement/service courants) n'est
 * jamais fourni par le client : {@code MovementService} le capture depuis
 * l'immobilisation elle-meme au moment de la demande, meme discipline que
 * la verification d'appartenance a la campagne en Phase 7 - jamais
 * declaratif cote client sur une donnee que le backend peut connaitre
 * lui-meme.
 */
public record MovementCreateRequest(
        @NotNull UUID assetId,
        @NotNull MovementType movementType,
        UUID toSiteId,
        UUID toLocationId,
        UUID toUserId,
        @Size(max = 150) String toDirection,
        @Size(max = 150) String toDepartment,
        @Size(max = 150) String toService,
        @Size(max = 2000) String reason,
        @Size(max = 2000) String comment
) {
}
