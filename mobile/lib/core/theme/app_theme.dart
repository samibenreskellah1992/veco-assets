/// Thème global VECO Assets Mobile (clair + sombre).
///
/// Assemble [AppColors], [AppTypography], [AppSpacing] et [AppRadius] en
/// deux [ThemeData] complets. Ne jamais construire de [ThemeData] ailleurs
/// dans l'app : toujours passer par [AppTheme.light] / [AppTheme.dark].
library;

import 'package:flutter/material.dart';

import 'app_colors.dart';
import 'app_radius.dart';
import 'app_typography.dart';

abstract final class AppTheme {
  static ThemeData get light => _build(
        brightness: Brightness.light,
        background: AppColors.lightBackground,
        foreground: AppColors.lightForeground,
        primary: AppColors.lightPrimary,
        primaryForeground: AppColors.lightPrimaryForeground,
        secondary: AppColors.lightSecondary,
        secondaryForeground: AppColors.lightSecondaryForeground,
        destructive: AppColors.lightDestructive,
        destructiveForeground: AppColors.lightDestructiveForeground,
        border: AppColors.borderLight,
      );

  static ThemeData get dark => _build(
        brightness: Brightness.dark,
        background: AppColors.darkBackground,
        foreground: AppColors.darkForeground,
        primary: AppColors.darkPrimary,
        primaryForeground: AppColors.darkPrimaryForeground,
        secondary: AppColors.darkSecondary,
        secondaryForeground: AppColors.darkSecondaryForeground,
        destructive: AppColors.darkDestructive,
        destructiveForeground: AppColors.darkDestructiveForeground,
        border: AppColors.borderDark,
      );

  static ThemeData _build({
    required Brightness brightness,
    required Color background,
    required Color foreground,
    required Color primary,
    required Color primaryForeground,
    required Color secondary,
    required Color secondaryForeground,
    required Color destructive,
    required Color destructiveForeground,
    required Color border,
  }) {
    final colorScheme = ColorScheme(
      brightness: brightness,
      primary: primary,
      onPrimary: primaryForeground,
      secondary: secondary,
      onSecondary: secondaryForeground,
      error: destructive,
      onError: destructiveForeground,
      surface: background,
      onSurface: foreground,
      outline: border,
    );

    final textTheme = TextTheme(
      headlineSmall: AppTypography.screenTitle.copyWith(color: foreground),
      titleLarge: AppTypography.sectionTitle.copyWith(color: foreground),
      titleMedium: AppTypography.cardTitle.copyWith(color: foreground),
      bodyMedium: AppTypography.bodyText.copyWith(color: foreground),
      bodySmall: AppTypography.secondaryText.copyWith(
        color: foreground.withValues(alpha: 0.7),
      ),
      labelLarge: AppTypography.label.copyWith(color: foreground),
      labelSmall: AppTypography.caption.copyWith(
        color: foreground.withValues(alpha: 0.6),
      ),
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: background,
      textTheme: textTheme,
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        foregroundColor: foreground,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: AppTypography.sectionTitle.copyWith(color: foreground),
      ),
      cardTheme: CardThemeData(
        color: brightness == Brightness.light
            ? AppColors.lightBackground
            : AppColors.darkSecondary,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: AppRadius.mdBorder,
          side: BorderSide(color: border, width: 1),
        ),
      ),
      dividerTheme: DividerThemeData(color: border, thickness: 1, space: 1),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: secondary,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: AppRadius.mdBorder,
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: AppRadius.mdBorder,
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppRadius.mdBorder,
          borderSide: BorderSide(color: primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: AppRadius.mdBorder,
          borderSide: BorderSide(color: destructive, width: 1.5),
        ),
        labelStyle: AppTypography.label.copyWith(
          color: foreground.withValues(alpha: 0.7),
        ),
        hintStyle: AppTypography.bodyText.copyWith(
          color: foreground.withValues(alpha: 0.4),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: primaryForeground,
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: AppRadius.mdBorder),
          textStyle: AppTypography.label,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: primary,
          side: BorderSide(color: border),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: AppRadius.mdBorder),
          textStyle: AppTypography.label,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: primary,
          textStyle: AppTypography.label,
        ),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: background,
        selectedItemColor: primary,
        unselectedItemColor: foreground.withValues(alpha: 0.5),
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: secondary,
        labelStyle: AppTypography.caption.copyWith(color: secondaryForeground),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        shape: RoundedRectangleBorder(borderRadius: AppRadius.pillBorder),
        side: BorderSide.none,
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: foreground,
        contentTextStyle: AppTypography.bodyText.copyWith(color: background),
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: AppRadius.mdBorder),
      ),
    );
  }
}
