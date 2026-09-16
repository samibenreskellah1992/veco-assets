package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityNameAndEntityId(String entityName, UUID entityId);

    // Phase 10 (optimisation N+1) : AuditLogMapper.toDto derefence
    // log.getUser() par ligne - utilise par AuditLogService.recent(), lui
    // meme appele par DashboardService pour l'activite recente (Phase 9).
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<AuditLog> findAll(Pageable pageable);
}
