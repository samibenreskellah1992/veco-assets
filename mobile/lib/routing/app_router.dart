/// Configuration GoRouter de VECO Assets Mobile.
///
/// Navigation volontairement impérative (context.go(...) explicite depuis
/// Splash/Login/Profil) plutôt qu'un `redirect` GoRouter réactif sur
/// l'état d'authentification : plus simple à raisonner pour ce périmètre
/// (une seule route protégée, `/`) et ça évite d'introduire tout de suite
/// la mécanique `refreshListenable` + Riverpod, plus fragile à écrire à
/// l'aveugle sans pouvoir exécuter Flutter moi-même. À revisiter si
/// d'autres routes protégées apparaissent (ex. deep links).
library;

import 'package:go_router/go_router.dart';

import '../features/auth/presentation/login_screen.dart';
import '../features/auth/presentation/splash_screen.dart';
import 'main_shell.dart';

final appRouter = GoRouter(
  initialLocation: '/splash',
  routes: [
    GoRoute(
      path: '/splash',
      builder: (context, state) => const SplashScreen(),
    ),
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/',
      builder: (context, state) => const MainShell(),
    ),
  ],
);
