import '../domain/user_summary.dart';

/// Reflète `LoginResponse` côté backend (dz.vecopharm.vecoassets.dto) :
/// `{ accessToken, tokenType, expiresInSeconds, user }`.
class LoginResult {
  const LoginResult({
    required this.accessToken,
    required this.tokenType,
    required this.expiresInSeconds,
    required this.user,
  });

  final String accessToken;
  final String tokenType;
  final int expiresInSeconds;
  final UserSummary user;

  factory LoginResult.fromJson(Map<String, dynamic> json) {
    return LoginResult(
      accessToken: json['accessToken'] as String,
      tokenType: json['tokenType'] as String,
      expiresInSeconds: json['expiresInSeconds'] as int,
      user: UserSummary.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}
