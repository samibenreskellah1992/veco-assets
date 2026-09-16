package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.ResetPasswordRequest;
import dz.vecopharm.vecoassets.dto.UserCreateRequest;
import dz.vecopharm.vecoassets.dto.UserDto;
import dz.vecopharm.vecoassets.dto.UserUpdateRequest;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Role;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.UserMapper;
import dz.vecopharm.vecoassets.repository.RoleRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import dz.vecopharm.vecoassets.entity.UserStatus;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Administration > Utilisateurs (prompt maitre Phase 4). Aucune suppression,
 * meme physique : un compte est desactive ({@link UserStatus#INACTIVE}),
 * jamais efface - contrairement au referentiel purement geographique, un
 * utilisateur est reference par l'audit trail (qui a fait quoi) et
 * potentiellement par des immobilisations (utilisateur courant/responsable);
 * le supprimer casserait cette tracabilite (prompt maitre section 25).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;

    public UserService(
            UserRepository userRepository,
            SiteRepository siteRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuditRecorder auditRecorder
    ) {
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName", "firstName")).stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto findById(UUID id) {
        return userMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public UserDto create(UserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Un utilisateur avec l'email '" + email + "' existe deja");
        }
        String matricule = normalizeMatricule(request.matricule());
        if (matricule != null && userRepository.existsByMatriculeIgnoreCase(matricule)) {
            throw new BusinessRuleException("Un utilisateur avec le matricule '" + matricule + "' existe deja");
        }

        User user = new User();
        user.setMatricule(matricule);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setSite(resolveSite(request.siteId()));
        user.setDepartment(request.department());
        user.setService(request.service());
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(resolveRoles(request.roleCodes()));

        user = userRepository.save(user);
        auditRecorder.record(AuditAction.CREATION, "ADMIN", "users", user.getId(), null, userMapper.toDto(user));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto update(UUID id, UserUpdateRequest request) {
        User user = getOrThrow(id);

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessRuleException("Un utilisateur avec l'email '" + email + "' existe deja");
        }
        String matricule = normalizeMatricule(request.matricule());
        if (matricule != null && userRepository.existsByMatriculeIgnoreCaseAndIdNot(matricule, id)) {
            throw new BusinessRuleException("Un utilisateur avec le matricule '" + matricule + "' existe deja");
        }

        UserDto before = userMapper.toDto(user);
        user.setMatricule(matricule);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setSite(resolveSite(request.siteId()));
        user.setDepartment(request.department());
        user.setService(request.service());
        user.setStatus(request.status());
        user.setRoles(resolveRoles(request.roleCodes()));

        auditRecorder.record(AuditAction.MODIFICATION, "ADMIN", "users", user.getId(), before, userMapper.toDto(user));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto setStatus(UUID id, UserStatus status) {
        User user = getOrThrow(id);
        if (user.getStatus() == status) {
            return userMapper.toDto(user);
        }
        UserDto before = userMapper.toDto(user);
        user.setStatus(status);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "ADMIN", "users", user.getId(), before, userMapper.toDto(user));
        return userMapper.toDto(user);
    }

    /**
     * Fixe par un administrateur (aucune notification email en V1, hors
     * perimetre - docs/ROADMAP.md section 12). Le mot de passe n'apparait
     * jamais dans l'audit trail : {@link UserDto} n'a pas de champ pour lui.
     */
    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request) {
        User user = getOrThrow(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        UserDto snapshot = userMapper.toDto(user);
        auditRecorder.record(AuditAction.MODIFICATION, "ADMIN", "users", user.getId(), snapshot, snapshot);
    }

    private Site resolveSite(UUID siteId) {
        if (siteId == null) {
            return null;
        }
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        Set<Role> roles = new HashSet<>();
        for (String code : roleCodes) {
            Role role = roleRepository.findByCode(code)
                    .orElseThrow(() -> new BusinessRuleException("Role inconnu : '" + code + "'"));
            roles.add(role);
        }
        return roles;
    }

    private String normalizeMatricule(String matricule) {
        return StringUtils.hasText(matricule) ? matricule.trim() : null;
    }

    private User getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
