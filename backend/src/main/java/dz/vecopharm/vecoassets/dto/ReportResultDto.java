package dz.vecopharm.vecoassets.dto;

import java.time.Instant;
import java.util.List;

/**
 * Resultat generique d'un rapport (prompt maitre Phase 9) : en-tetes +
 * lignes deja formatees en texte d'affichage (dates/montants localises).
 * Delibérement generique - un seul type de DTO pour les 11 {@link ReportType}
 * - pour que le tableau affiche a l'ecran et les exports CSV/Excel/PDF
 * partagent exactement les memes donnees calculees une seule fois : c'est
 * ce qui garantit "chaque chiffre du rapport est verifiable" (il ne peut
 * pas y avoir de divergence entre ce que l'utilisateur voit et ce qu'il
 * telecharge, contrairement a deux implementations separees).
 */
public record ReportResultDto(
        ReportType type,
        String title,
        Instant generatedAt,
        List<String> columns,
        List<List<String>> rows
) {
}
