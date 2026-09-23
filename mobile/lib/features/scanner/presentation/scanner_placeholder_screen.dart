import 'package:flutter/material.dart';

import '../../../core/widgets/phase_placeholder.dart';

/// Voir docs/MOBILE-PHASE-0.md : le scan (mobile_scanner + comparaison
/// LOC-xxx / VECO-IMM-xxx) est développé à la Phase 5, en même temps que
/// l'ajout de `GET /api/assets/by-code/{code}` côté backend (décision de
/// Sami, section 13 du document).
class ScannerPlaceholderScreen extends StatelessWidget {
  const ScannerPlaceholderScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PhasePlaceholder(
      title: 'Scanner',
      icon: Icons.qr_code_scanner,
      phaseLabel: 'Phase 5 — Scanner',
    );
  }
}
