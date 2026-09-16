package dz.vecopharm.vecoassets.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables {@code @CreatedDate}/{@code @LastModifiedDate} auto-population on
 * {@link dz.vecopharm.vecoassets.entity.BaseEntity} and
 * {@link dz.vecopharm.vecoassets.entity.BaseCreatedEntity}. Author tracking
 * ({@code @CreatedBy}/{@code @LastModifiedBy}) is added in Phase 3 once the
 * authenticated principal is available.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
