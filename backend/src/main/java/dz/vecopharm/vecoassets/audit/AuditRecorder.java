package dz.vecopharm.vecoassets.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.AuditLog;
import dz.vecopharm.vecoassets.repository.AuditLogRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/**
 * Piste d'audit generique (prompt maitre section 25/52) : qui / quoi / quand /
 * ancienne-nouvelle valeur, pour toute action critique. Centralise ici ce que
 * {@code AuthService} construisait a la main en Phase 3 pour la connexion,
 * afin que les services Phase 4+ (referentiel, utilisateurs, puis
 * immobilisations/inventaire/mouvements dans les phases suivantes) n'aient
 * pas chacun a reecrire la construction d'un {@link AuditLog}.
 *
 * L'utilisateur courant et l'adresse IP sont resolus depuis le contexte de
 * la requete HTTP en cours (jamais passes en clair par l'appelant), ce qui
 * garde les services metier concentres sur leur propre logique.
 */
@Component
public class AuditRecorder {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditRecorder(AuditLogRepository auditLogRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public void record(AuditAction action, String module, String entityName, UUID entityId, Object oldValue, Object newValue) {
        AuditLog log = new AuditLog();
        currentUserId().ifPresent(email -> userRepository.findByEmail(email).ifPresent(log::setUser));
        log.setAction(action);
        log.setModule(module);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setOldValue(toJson(oldValue));
        log.setNewValue(toJson(newValue));
        log.setIpAddress(currentIpAddress());
        log.setOccurredAt(Instant.now());
        auditLogRepository.save(log);
    }

    private java.util.Optional<String> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(authentication.getName());
    }

    private String currentIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }
        return null;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "\"(non serialisable: " + ex.getOriginalMessage() + ")\"";
        }
    }
}
