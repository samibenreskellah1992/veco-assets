// ignore_for_file: prefer_initializing_formals
//
// Constructeur volontairement écrit avec des paramètres nommés publics
// (`api`/`tokenStorage`) distincts des champs privés (`_api`/
// `_tokenStorage`) plutôt qu'avec des initializing formals (`this._api`) :
// un initializing formal sur un champ privé rendrait le nom du paramètre
// nommé lui-même privé (`_api`), donc inutilisable depuis un autre fichier
// (auth_providers.dart) qui construit `AuthRepository(api: ...,
// tokenStorage: ...)`. Le hint de l'analyzer ne s'applique pas ici.

/// Combine [AuthApi] (réseau) et [TokenStorage] (persistance sécurisée du
/// JWT) : c'est ce repository, pas l'API brute, que le contrôleur d'état
/// (Riverpod) doit utiliser.
library;

import '../../../core/storage/token_storage.dart';
import '../domain/user_summary.dart';
import 'auth_api.dart';

class AuthRepository {
  AuthRepository({required AuthApi api, required TokenStorage tokenStorage})
      : _api = api,
        _tokenStorage = tokenStorage;

  final AuthApi _api;
  final TokenStorage _tokenStorage;

  /// Se connecte, persiste le token en stockage sécurisé, et retourne le
  /// profil utilisateur.
  Future<UserSummary> login({
    required String email,
    required String password,
  }) async {
    final result = await _api.login(email: email, password: password);
    await _tokenStorage.saveToken(result.accessToken);
    return result.user;
  }

  /// Lit le token stocké et, s'il existe, le valide auprès du backend via
  /// `/api/auth/me`. Retourne `null` si aucune session valide (pas de
  /// token, ou token expiré/rejeté par le backend).
  Future<UserSummary?> restoreSession() async {
    final token = await _tokenStorage.readToken();
    if (token == null) return null;

    try {
      return await _api.me();
    } catch (_) {
      // Token expiré/invalide côté backend : on nettoie et on retombe sur
      // l'écran de connexion plutôt que de laisser l'app dans un état
      // incohérent.
      await _tokenStorage.clear();
      return null;
    }
  }

  Future<void> logout() => _tokenStorage.clear();
}
