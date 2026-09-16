package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.RoleDto;
import dz.vecopharm.vecoassets.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lecture seule : les 6 roles V1 (prompt maitre section 27) sont fixes,
 * pas de CRUD role en V1. Sert a peupler le formulaire d'affectation de
 * roles dans Administration > Utilisateurs (Phase 4) - reserve a
 * USER_MANAGE comme {@code UserController}, meme si {@link RoleDto}
 * n'expose pas les permissions attachees a chaque role : la taxonomie des
 * roles n'a pas a etre visible aux comptes qui ne gerent pas les
 * utilisateurs (correction Phase 10, cette methode n'avait auparavant
 * aucune annotation d'autorisation au-dela de l'authentification globale -
 * seule breche trouvee lors de la revue de tous les controleurs de
 * l'application).
 */
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<RoleDto> findAll() {
        return roleService.findAll();
    }
}
