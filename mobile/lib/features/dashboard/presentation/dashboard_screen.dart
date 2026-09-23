/// Écran Tableau de bord (Phase 3, données mock — section 57 du prompt
/// maître). Le câblage sur `GET /api/dashboard` (déjà disponible côté
/// backend, voir docs/MOBILE-PHASE-0.md section 8) viendra remplacer ces
/// données mock à la Phase 3 avancée / avec le module Sync.
library;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/stat_tile.dart';
import '../../auth/presentation/auth_providers.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final user = ref.watch(authControllerProvider).user;
    final firstName = (user?.fullName ?? '').split(' ').first;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Tableau de bord'),
        actions: [
          IconButton(
            icon: const Icon(Icons.cloud_off_outlined),
            tooltip: 'État de synchronisation',
            onPressed: () {},
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.screenPadding),
        children: [
          Text(
            firstName.isNotEmpty ? 'Bonjour, $firstName' : 'Bonjour',
            style: AppTypography.screenTitle,
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            'Voici un aperçu du parc immobilisé VECOPHARM.',
            style: theme.textTheme.bodyMedium,
          ),
          const SizedBox(height: AppSpacing.sectionGap),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: AppSpacing.sm,
            crossAxisSpacing: AppSpacing.sm,
            childAspectRatio: 1.4,
            children: const [
              StatTile(
                value: '1 284',
                label: 'Immobilisations actives',
                icon: Icons.inventory_2_outlined,
              ),
              StatTile(
                value: '7',
                label: 'Anomalies ouvertes',
                icon: Icons.report_gmailerrorred_outlined,
                accentColor: Color(0xFFDC2828),
              ),
              StatTile(
                value: '3',
                label: 'Mouvements en attente',
                icon: Icons.swap_horiz_outlined,
                accentColor: Color(0xFFF59F0A),
              ),
              StatTile(
                value: '2',
                label: 'Inventaires en cours',
                icon: Icons.fact_check_outlined,
                accentColor: Color(0xFF1A9948),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sectionGap),
          Text('Actions rapides', style: AppTypography.sectionTitle),
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              Expanded(
                child: FilledButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.qr_code_scanner),
                  label: const Text('Scanner'),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.location_on_outlined),
                  label: const Text('Locaux'),
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sectionGap),
          Text('Activité récente', style: AppTypography.sectionTitle),
          const SizedBox(height: AppSpacing.sm),
          const _RecentActivityCard(
            icon: Icons.qr_code_scanner,
            title: 'Local LOC-ALG-014 scanné',
            subtitle: '18 immobilisations conformes, 1 anomalie',
            time: 'Il y a 32 min',
          ),
          const SizedBox(height: AppSpacing.sm),
          const _RecentActivityCard(
            icon: Icons.swap_horiz_outlined,
            title: 'Mouvement validé',
            subtitle: 'VECO-IMM-00231 — Bureau 2A vers Entrepôt B',
            time: 'Il y a 2 h',
          ),
          const SizedBox(height: AppSpacing.sm),
          const _RecentActivityCard(
            icon: Icons.fact_check_outlined,
            title: 'Inventaire terminé',
            subtitle: 'Site Alger — Bâtiment Administratif',
            time: 'Hier',
          ),
        ],
      ),
    );
  }
}

class _RecentActivityCard extends StatelessWidget {
  const _RecentActivityCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.time,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final String time;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return AppCard(
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: theme.colorScheme.secondary,
            foregroundColor: theme.colorScheme.onSecondary,
            child: Icon(icon, size: 20),
          ),
          const SizedBox(width: AppSpacing.sm),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: AppTypography.cardTitle),
                const SizedBox(height: 2),
                Text(subtitle, style: theme.textTheme.bodySmall),
              ],
            ),
          ),
          Text(time, style: theme.textTheme.labelSmall),
        ],
      ),
    );
  }
}
