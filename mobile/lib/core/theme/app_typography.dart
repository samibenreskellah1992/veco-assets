/// Hiérarchie typographique VECO Assets Mobile.
///
/// Échelle de styles cohérente (section 45 du prompt maître) : titre
/// écran, titre section, titre card, texte principal, texte secondaire,
/// caption. Les couleurs ne sont PAS fixées ici (elles dépendent du
/// thème clair/sombre) : [AppTheme] applique [AppColors] par-dessus ces
/// styles via `TextTheme.apply(...)` / des copies locales.
library;

import 'package:flutter/material.dart';

abstract final class AppTypography {
  /// Titre d'écran (ex. "Tableau de bord", "Scanner").
  static const TextStyle screenTitle = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w700,
    height: 1.25,
    letterSpacing: -0.2,
  );

  /// Titre de section à l'intérieur d'un écran.
  static const TextStyle sectionTitle = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w600,
    height: 1.3,
  );

  /// Titre d'une carte (asset, local, mouvement...).
  static const TextStyle cardTitle = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.3,
  );

  /// Texte principal / corps de texte.
  static const TextStyle bodyText = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.45,
  );

  /// Texte secondaire (métadonnées, sous-titres de carte).
  static const TextStyle secondaryText = TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w400,
    height: 1.4,
  );

  /// Libellés de bouton / champ.
  static const TextStyle label = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w500,
    height: 1.2,
  );

  /// Légende / texte le plus discret (horodatage, code, compteur).
  static const TextStyle caption = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w400,
    height: 1.3,
    letterSpacing: 0.1,
  );

  /// Chiffres mis en avant (tableau de bord, badges de comptage).
  static const TextStyle statNumber = TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.w700,
    height: 1.1,
  );
}
