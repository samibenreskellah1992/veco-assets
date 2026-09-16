package dz.vecopharm.vecoassets.dto;

/**
 * Une ligne de repartition generique ("label" -> nombre d'immobilisations),
 * utilisee pour les graphiques du tableau de bord (par site / par
 * categorie / par etat - prompt maitre Phase 9). Volontairement generique
 * plutot qu'un DTO par axe : le frontend affiche les trois avec le meme
 * composant de graphique en barres.
 */
public record CountByLabelDto(String label, long count) {
}
