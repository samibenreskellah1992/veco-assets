/// Écran de démarrage : logo VECOPHARM (variante blanche) sur fond marine
/// de marque, le temps de vérifier s'il existe une session valide.
///
/// Fond fixe (couleur `sidebar` du Design System) plutôt que dépendant du
/// thème clair/sombre : un splash de marque garde la même identité
/// visuelle quel que soit le mode du téléphone.
library;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import 'auth_providers.dart';
import 'auth_state.dart';

class SplashScreen extends ConsumerStatefulWidget {
  const SplashScreen({super.key});

  @override
  ConsumerState<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends ConsumerState<SplashScreen> {
  @override
  void initState() {
    super.initState();
    // Différé après le premier frame : évite de déclencher une navigation
    // pendant la construction de l'arbre de widgets.
    WidgetsBinding.instance.addPostFrameCallback((_) => _checkSession());
  }

  Future<void> _checkSession() async {
    await ref.read(authControllerProvider.notifier).restoreSession();
    if (!mounted) return;

    final status = ref.read(authControllerProvider).status;
    if (status == AuthStatus.authenticated) {
      context.go('/');
    } else {
      context.go('/login');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.lightSidebar,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Image.asset(
              'assets/images/vecopharm_logo_white.png',
              width: 180,
            ),
            const SizedBox(height: AppSpacing.xl),
            const SizedBox(
              width: 28,
              height: 28,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                valueColor: AlwaysStoppedAnimation<Color>(
                  AppColors.lightSidebarMuted,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
