import 'package:flutter/material.dart';

import '../../../core/widgets/phase_placeholder.dart';

/// Le module Locaux (liste, détail, génération QR) consomme l'API
/// `/api/locations/...` déjà livrée côté backend (Checkpoints 1-3) ; son
/// écriture mobile est prévue Phase 4, après l'authentification.
class LocationsPlaceholderScreen extends StatelessWidget {
  const LocationsPlaceholderScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PhasePlaceholder(
      title: 'Locaux',
      icon: Icons.location_on_outlined,
      phaseLabel: 'Phase 4 — Locaux',
    );
  }
}
