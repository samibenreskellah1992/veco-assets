/// Configuration GoRouter de VECO Assets Mobile.
///
/// État actuel (Phase 3) : une seule route racine vers [MainShell], qui
/// gère lui-même ses 4 onglets en interne (pas de sous-routes GoRouter
/// pour l'instant — inutile tant que chaque écran n'a pas de navigation
/// interne propre, ex. Locaux -> Détail Local).
///
/// À la Phase 2 (authentification, en attente des fichiers logo
/// VECOPHARM), ce fichier gagnera :
///   - les routes `/splash` et `/login` ;
///   - une fonction `redirect` basée sur l'état d'authentification
///     (token JWT présent en flutter_secure_storage ou non) pour protéger
///     la route racine.
/// Ne pas anticiper cette logique maintenant : la règle de non-invention
/// (section 61 du prompt maître) s'applique aussi à l'architecture, pas
/// seulement aux règles métier.
library;

import 'package:go_router/go_router.dart';

import 'main_shell.dart';

final appRouter = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const MainShell(),
    ),
  ],
);
