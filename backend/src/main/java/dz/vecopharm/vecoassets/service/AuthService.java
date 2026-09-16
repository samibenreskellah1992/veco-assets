package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.LoginRequest;
import dz.vecopharm.vecoassets.dto.LoginResponse;
import dz.vecopharm.vecoassets.dto.UserSummaryDto;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.AuditLog;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.UserMapper;
import dz.vecopharm.vecoassets.repository.AuditLogRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import dz.vecopharm.vecoassets.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Login flow: prompt maitre section 4 (auth locale + JWT) and section 25
 * (chaque connexion doit etre auditee). No business logic lives in
 * {@code AuthController} - it only translates HTTP <-> this service.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserMapper userMapper;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            UserMapper userMapper
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        // Throws BadCredentialsException / DisabledException on failure,
        // caught by GlobalExceptionHandler.handleAuthentication -> 401.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailWithRolesAndPermissions(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        String token = jwtService.generateToken(user, authoritiesOf(authentication));

        user.setLastLoginAt(Instant.now());
        auditLogRepository.save(connexionAuditLog(user, ipAddress));

        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), userMapper.toSummary(user));
    }

    @Transactional(readOnly = true)
    public UserSummaryDto currentUser(String email) {
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return userMapper.toSummary(user);
    }

    private java.util.List<String> authoritiesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private AuditLog connexionAuditLog(User user, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(AuditAction.CONNEXION);
        log.setModule("AUTH");
        log.setEntityName("users");
        log.setEntityId(user.getId());
        log.setIpAddress(ipAddress);
        log.setOccurredAt(Instant.now());
        return log;
    }
}
