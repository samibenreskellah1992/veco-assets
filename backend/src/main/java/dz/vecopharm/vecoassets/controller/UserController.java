package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.ResetPasswordRequest;
import dz.vecopharm.vecoassets.dto.UserCreateRequest;
import dz.vecopharm.vecoassets.dto.UserDto;
import dz.vecopharm.vecoassets.dto.UserUpdateRequest;
import dz.vecopharm.vecoassets.entity.UserStatus;
import dz.vecopharm.vecoassets.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Referentiel > Utilisateurs (prompt maitre Phase 4). Reserve a
 * USER_MANAGE - contrairement aux autres ressources du referentiel, la
 * liste des utilisateurs (telephone, matricule, service...) n'est pas
 * ouverte a tout utilisateur authentifie.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserDto findById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    public UserDto activate(@PathVariable UUID id) {
        return userService.setStatus(id, UserStatus.ACTIVE);
    }

    @PostMapping("/{id}/deactivate")
    public UserDto deactivate(@PathVariable UUID id) {
        return userService.setStatus(id, UserStatus.INACTIVE);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
