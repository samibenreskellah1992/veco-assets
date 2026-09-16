package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.dto.AuditLogDto;
import dz.vecopharm.vecoassets.mapper.AuditLogMapper;
import dz.vecopharm.vecoassets.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consultation de l'audit trail (prompt maitre section 25/52). Ecriture
 * exclusivement depuis les services metier qui produisent les evenements
 * (voir {@link AuthService}) - ce service ne fait que lire.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> recent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return auditLogRepository
                .findAll(PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "occurredAt")))
                .map(auditLogMapper::toDto)
                .toList();
    }
}
