/// Câblage Riverpod du module Authentification : Dio -> AuthApi ->
/// AuthRepository -> AuthController (état exposé à toute l'app).
library;

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/storage/token_storage.dart';
import '../data/auth_api.dart';
import '../data/auth_repository.dart';
import 'auth_state.dart';

final dioProvider = Provider<Dio>((ref) => DioClient().dio);

final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());

final authApiProvider = Provider<AuthApi>((ref) => AuthApi(ref.watch(dioProvider)));

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    api: ref.watch(authApiProvider),
    tokenStorage: ref.watch(tokenStorageProvider),
  );
});

class AuthController extends Notifier<AuthState> {
  @override
  AuthState build() => const AuthState.checking();

  /// Appelé une seule fois par [SplashScreen] : regarde s'il existe une
  /// session valide et met à jour l'état en conséquence.
  Future<void> restoreSession() async {
    final repository = ref.read(authRepositoryProvider);
    final user = await repository.restoreSession();
    state = user != null
        ? AuthState.authenticated(user)
        : const AuthState.unauthenticated();
  }

  Future<void> login({required String email, required String password}) async {
    state = const AuthState.authenticating();
    try {
      final repository = ref.read(authRepositoryProvider);
      final user = await repository.login(email: email, password: password);
      state = AuthState.authenticated(user);
    } catch (e) {
      state = AuthState.unauthenticated(errorMessage: e.toString());
    }
  }

  Future<void> logout() async {
    await ref.read(authRepositoryProvider).logout();
    state = const AuthState.unauthenticated();
  }
}

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
