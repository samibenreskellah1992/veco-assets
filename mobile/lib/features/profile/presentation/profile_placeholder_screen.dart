import 'package:flutter/material.dart';

import '../../../core/widgets/phase_placeholder.dart';

/// Profil (identité utilisateur, déconnexion, préférences) : câblé à la
/// Phase 2 avec l'authentification, elle-même en attente des fichiers logo
/// VECOPHARM (décision de Sami, docs/MOBILE-PHASE-0.md section 13).
class ProfilePlaceholderScreen extends StatelessWidget {
  const ProfilePlaceholderScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PhasePlaceholder(
      title: 'Profil',
      icon: Icons.person_outline,
      phaseLabel: 'Phase 2 — Authentification',
    );
  }
}
