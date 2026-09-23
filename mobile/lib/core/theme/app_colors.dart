/// Palette de couleurs VECO Assets Mobile.
///
/// Ces valeurs sont reprises telles quelles des tokens HSL définis dans
/// `frontend/src/index.css` (design system shadcn/ui du web VECO Assets),
/// convertis en RGB, pour garantir une identité visuelle strictement
/// cohérente entre le web et le mobile plutôt que d'inventer une palette
/// approximative. Voir `docs/MOBILE-PHASE-0.md` section 4 pour le détail
/// des valeurs HSL sources.
///
/// Ne jamais utiliser de couleurs directement dans les widgets : toujours
/// passer par [AppColors] (ou par le [ColorScheme] exposé par [AppTheme]).
library;

import 'package:flutter/material.dart';

abstract final class AppColors {
  // ---------------------------------------------------------------------
  // Mode clair
  // ---------------------------------------------------------------------
  static const Color lightBackground = Color(0xFFFFFFFF);
  static const Color lightForeground = Color(0xFF0F1729);

  static const Color lightPrimary = Color(0xFF0950C3);
  static const Color lightPrimaryForeground = Color(0xFFF8FAFC);

  static const Color lightSecondary = Color(0xFFF1F5F9);
  static const Color lightSecondaryForeground = Color(0xFF0F1729);

  static const Color lightDestructive = Color(0xFFDC2828);
  static const Color lightDestructiveForeground = Color(0xFFF8FAFC);

  static const Color lightSuccess = Color(0xFF1A9948);
  static const Color lightSuccessForeground = Color(0xFFF8FAFC);

  static const Color lightWarning = Color(0xFFF59F0A);
  static const Color lightWarningForeground = Color(0xFF0F1729);

  /// Bleu foncé corporate VECOPHARM (Splash Screen, barre de navigation).
  static const Color lightSidebar = Color(0xFF0F1729);
  static const Color lightSidebarForeground = Color(0xFFF1F5F9);
  static const Color lightSidebarMuted = Color(0xFF94A3B8);
  static const Color lightSidebarActive = Color(0xFF0950C3);

  // ---------------------------------------------------------------------
  // Mode sombre
  // ---------------------------------------------------------------------
  static const Color darkBackground = Color(0xFF0B111E);
  static const Color darkForeground = Color(0xFFF8FAFC);

  static const Color darkPrimary = Color(0xFF3C83F6);
  static const Color darkPrimaryForeground = Color(0xFF0F1729);

  static const Color darkSecondary = Color(0xFF1D283A);
  static const Color darkSecondaryForeground = Color(0xFFF8FAFC);

  static const Color darkDestructive = Color(0xFF811D1D);
  static const Color darkDestructiveForeground = Color(0xFFF8FAFC);

  static const Color darkSuccess = Color(0xFF21C45D);
  static const Color darkSuccessForeground = Color(0xFF0F1729);

  static const Color darkWarning = Color(0xFFF59F0A);
  static const Color darkWarningForeground = Color(0xFF0F1729);

  static const Color darkSidebar = Color(0xFF080C16);
  static const Color darkSidebarForeground = Color(0xFFF1F5F9);
  static const Color darkSidebarMuted = Color(0xFF7588A3);
  static const Color darkSidebarActive = Color(0xFF3C83F6);

  // ---------------------------------------------------------------------
  // Neutres partagés (indépendants du thème)
  // ---------------------------------------------------------------------

  /// Utilisé pour les bordures/dividers discrets, dérivé de secondary.
  static const Color borderLight = Color(0xFFE2E8F0);
  static const Color borderDark = Color(0xFF334155);
}
