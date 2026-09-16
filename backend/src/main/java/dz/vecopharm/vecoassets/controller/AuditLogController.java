package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.AuditLogDto;
import dz.vecopharm.vecoassets.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administration > Audit (prompt maitre section 25/52). Reserve a
 * ADMIN_ACCESS - sert aussi de demonstration, en Phase 3, que les
 * permissions issues de la base (pas d'un role code en dur) sont
 * reellement appliquees cote backend (voir docs/ROADMAP.md Phase 3).
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public List<AuditLogDto> recent(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return auditLogService.recent(limit);
    }
}
