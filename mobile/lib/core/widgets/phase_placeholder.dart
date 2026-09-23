/// Écran-jalon pour un onglet dont la logique métier n'est pas encore
/// développée (phase mobile pas encore atteinte, voir la section 55 du
/// prompt maître pour l'ordre des phases).
///
/// Volontairement minimal : pas de données mock inventées pour un écran
/// dont l'API/les règles métier ne sont pas encore câblées, conformément à
/// la règle de non-invention (section 61 du prompt maître). Sert
/// uniquement à ce que la navigation reste démontrable de bout en bout dès
/// la Phase 3.
library;

import 'package:flutter/material.dart';

import '../theme/app_spacing.dart';

class PhasePlaceholder extends StatelessWidget {
  const PhasePlaceholder({
    super.key,
    required this.title,
    required this.icon,
    required this.phaseLabel,
  });

  final String title;
  final IconData icon;

  /// Ex. "Phase 4 — Locaux".
  final String phaseLabel;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 48, color: theme.colorScheme.outline),
              const SizedBox(height: AppSpacing.md),
              Text(
                'À venir',
                style: theme.textTheme.titleLarge,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                '$phaseLabel du développement mobile.',
                style: theme.textTheme.bodySmall,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
