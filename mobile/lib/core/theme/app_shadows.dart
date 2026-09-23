/// Ombres VECO Assets Mobile.
///
/// Ombres très discrètes uniquement (section 1 du prompt maître :
/// "ombres très discrètes") — jamais d'ombres marquées façon Material
/// par défaut, pour garder le rendu premium/B2B de la référence visuelle.
library;

import 'package:flutter/material.dart';

abstract final class AppShadows {
  static const List<BoxShadow> card = [
    BoxShadow(
      color: Color(0x0F0F1729),
      blurRadius: 8,
      offset: Offset(0, 2),
    ),
  ];

  static const List<BoxShadow> raised = [
    BoxShadow(
      color: Color(0x140F1729),
      blurRadius: 16,
      offset: Offset(0, 4),
    ),
  ];

  static const List<BoxShadow> none = [];
}
