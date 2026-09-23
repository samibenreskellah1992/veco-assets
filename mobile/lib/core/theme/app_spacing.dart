/// Échelle d'espacement VECO Assets Mobile.
///
/// Espacement généreux et cohérent (section 3/45 du prompt maître) :
/// toujours utiliser ces constantes plutôt que des valeurs magiques dans
/// les widgets, pour garder une grille visuelle homogène sur toute
/// l'application.
library;

abstract final class AppSpacing {
  static const double xs = 4;
  static const double sm = 8;
  static const double md = 16;
  static const double lg = 24;
  static const double xl = 32;
  static const double xxl = 48;

  /// Marge latérale standard des écrans (respecte la zone tactile sur
  /// petit écran, voir section 46 du prompt maître).
  static const double screenPadding = 16;

  /// Espacement vertical standard entre deux sections d'un écran.
  static const double sectionGap = 24;
}
