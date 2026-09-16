package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.LoginRequest;
import dz.vecopharm.vecoassets.dto.LoginResponse;
import dz.vecopharm.vecoassets.dto.UserSummaryDto;
import dz.vecopharm.vecoassets.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    /** Profil de l'utilisateur authentifie (email = subject du JWT) - utilise par le frontend au demarrage. */
    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> me(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication.getName()));
    }
}
