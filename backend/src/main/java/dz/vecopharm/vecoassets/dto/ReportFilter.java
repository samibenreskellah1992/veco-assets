package dz.vecopharm.vecoassets.dto;

import dz.vecopharm.vecoassets.entity.AnomalyStatus;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.MovementType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtres communs a tous les rapports (prompt maitre Phase 9) - chaque
 * {@link ReportType} n'utilise que le sous-ensemble qui le concerne
 * (documente dans {@link ReportService}), un champ non pertinent pour un
 * type donne est simplement ignore plutot que de rejeter la requete : ca
 * permet au frontend de garder une seule barre de filtres et de ne montrer
 * que les champs pertinents, sans que le contrat d'API ne change par type.
 */
public record ReportFilter(
        UUID siteId,
        UUID categoryId,
        AssetCondition condition,
        AssetStatus status,
        MovementType movementType,
        AnomalyStatus anomalyStatus,
        LocalDate dateFrom,
        LocalDate dateTo
) {
}
