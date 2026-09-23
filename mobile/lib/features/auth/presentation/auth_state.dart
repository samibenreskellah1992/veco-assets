import '../domain/user_summary.dart';

enum AuthStatus {
  /// Vérification initiale (session stockée ?) en cours — écran Splash.
  checking,
  authenticating,
  authenticated,
  unauthenticated,
}

class AuthState {
  const AuthState({required this.status, this.user, this.errorMessage});

  const AuthState.checking() : this(status: AuthStatus.checking);
  const AuthState.unauthenticated({String? errorMessage})
      : this(status: AuthStatus.unauthenticated, errorMessage: errorMessage);
  const AuthState.authenticating() : this(status: AuthStatus.authenticating);
  const AuthState.authenticated(UserSummary user)
      : this(status: AuthStatus.authenticated, user: user);

  final AuthStatus status;
  final UserSummary? user;
  final String? errorMessage;

  bool get isAuthenticated => status == AuthStatus.authenticated;
}
