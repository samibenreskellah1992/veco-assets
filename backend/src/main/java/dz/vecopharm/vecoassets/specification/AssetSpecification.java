package dz.vecopharm.vecoassets.specification;

import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtres combinables pour la liste des immobilisations (prompt maitre
 * Phase 5) : chaque critere n'est applique que s'il est fourni, et tous se
 * combinent en ET - {@code JpaSpecificationExecutor} est deja branche sur
 * {@code AssetRepository} depuis la Phase 2 pour exactement cet usage.
 */
public final class AssetSpecification {

    private AssetSpecification() {
    }

    public static Specification<Asset> withFilters(
            boolean includeDeleted,
            UUID siteId,
            UUID categoryId,
            UUID locationId,
            AssetCondition condition,
            AssetStatus status,
            Boolean labeled,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("deleted")));
            }
            if (siteId != null) {
                predicates.add(cb.equal(root.get("site").get("id"), siteId));
            }
            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("id"), locationId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (condition != null) {
                predicates.add(cb.equal(root.get("condition"), condition));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (labeled != null) {
                predicates.add(cb.equal(root.get("labeled"), labeled));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("designation")), pattern),
                        cb.like(cb.lower(root.<String>get("assetCode")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("serialNumber"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
