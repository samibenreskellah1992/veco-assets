package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.RoleDto;
import dz.vecopharm.vecoassets.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lecture seule : les 6 roles V1 (prompt maitre section 27) sont fixes,
 * pas de CRUD role en V1. Sert a peupler le formulaire d'affectation de
 * roles dans Administration > Utilisateurs (Phase 4).
 */
@RestController
@RequestMapping("/api/roles")
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
