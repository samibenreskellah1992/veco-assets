package dz.vecopharm.vecoassets.controller;

import dz.vecopharm.vecoassets.dto.DashboardDto;
import dz.vecopharm.vecoassets.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tableau de bord (prompt maitre Phase 9). Reserve a REPORT_VIEW (deja
 * possede par ADMIN/GESTIONNAIRE_PATRIMOINE/RESPONSABLE_SITE/RESPONSABLE_SERVICE/
 * CONSULTATION depuis la Phase 2 - seul INVENTORISTE ne l'a pas, son role
 * etant le scan, pas la consultation de rapports).
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public DashboardDto get() {
        return dashboardService.get();
    }
}
