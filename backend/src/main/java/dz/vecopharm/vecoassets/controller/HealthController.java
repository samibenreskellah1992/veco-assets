package dz.vecopharm.vecoassets.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Minimal liveness endpoint used to verify, during Phase 1, that the
 * backend actually starts and serves the REST layer. Spring Boot Actuator
 * also exposes /actuator/health for infrastructure-level checks.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "application", "veco-assets",
                "timestamp", Instant.now().toString()
        );
    }
}
