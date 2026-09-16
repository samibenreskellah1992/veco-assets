package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.MovementCreateRequest;
import dz.vecopharm.vecoassets.dto.MovementDto;
import dz.vecopharm.vecoassets.dto.MovementRejectRequest;
import dz.vecopharm.vecoassets.entity.MovementStatus;
import dz.vecopharm.vecoassets.entity.MovementType;
import dz.vecopharm.vecoassets.service.MovementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Mouvements d'immobilisation (prompt maitre Phase 8). Lecture reservee a
 * {@code IMMOBILISATION_VIEW} (meme convention que {@code /api/assets/{id}/
 * assignments}/{@code status-history} en Phase 5 : un mouvement est une
 * donnee d'historique d'immobilisation, pas un module a permission de
 * lecture dediee - aucune {@code MOUVEMENT_VIEW} n'a ete seedee en Phase 2).
 * Creation reservee a {@code MOUVEMENT_CREATE} ; validation/rejet/execution
 * reserves a {@code MOUVEMENT_VALIDATE} - seuls GESTIONNAIRE_PATRIMOINE et
 * ADMIN possedent cette derniere permission (V3), RESPONSABLE_SITE/SERVICE
 * ne peuvent que demander.
 */
@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('IMMOBILISATION_VIEW')")
    public List<MovementDto> list(
            @RequestParam(name = "assetId", required = false) UUID assetId,
            @RequestParam(name = "status", required = false) MovementStatus status,
            @RequestParam(name = "type", required = false) MovementType type
    ) {
        return movementService.list(assetId, status, type);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('IMMOBILISATION_VIEW')")
    public MovementDto findById(@PathVariable UUID id) {
        return movementService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOUVEMENT_CREATE')")
    public ResponseEntity<MovementDto> request(@Valid @RequestBody MovementCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movementService.request(request));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('MOUVEMENT_VALIDATE')")
    public MovementDto validate(@PathVariable UUID id) {
        return movementService.validate(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MOUVEMENT_VALIDATE')")
    public MovementDto reject(@PathVariable UUID id, @Valid @RequestBody(required = false) MovementRejectRequest request) {
        return movementService.reject(id, request);
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('MOUVEMENT_VALIDATE')")
    public MovementDto execute(@PathVariable UUID id) {
        return movementService.execute(id);
    }
}
