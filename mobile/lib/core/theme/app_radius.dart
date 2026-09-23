/// Rayons de bordure VECO Assets Mobile.
///
/// Coins légèrement arrondis, cohérents avec le langage visuel de la
/// maquette de référence (section 1 du prompt maître : "cartes avec coins
/// légèrement arrondis").
library;

import 'package:flutter/widgets.dart';

abstract final class AppRadius {
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double pill = 999;

  static const BorderRadius smBorder = BorderRadius.all(Radius.circular(sm));
  static const BorderRadius mdBorder = BorderRadius.all(Radius.circular(md));
  static const BorderRadius lgBorder = BorderRadius.all(Radius.circular(lg));
  static const BorderRadius pillBorder =
      BorderRadius.all(Radius.circular(pill));
}
