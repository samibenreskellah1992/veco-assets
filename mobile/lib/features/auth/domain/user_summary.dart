/// Profil utilisateur — reflète exactement `UserSummaryDto` côté backend
/// (dz.vecopharm.vecoassets.dto.UserSummaryDto), renvoyé tel quel par
/// `POST /api/auth/login` (champ `user`) et `GET /api/auth/me`.
///
/// Classe manuscrite (pas de freezed/json_serializable) pour ce modèle
/// simple : le codegen sera introduit à partir des modèles plus riches
/// (Locaux, Immobilisations, Phase 4+) pour ne pas ajouter d'étape de
/// génération inutile à ce stade.
library;

class UserSummary {
  const UserSummary({
    required this.id,
    required this.matricule,
    required this.fullName,
    required this.email,
    this.siteName,
    this.department,
    this.service,
    required this.roles,
    required this.permissions,
  });

  final String id;
  final String matricule;
  final String fullName;
  final String email;
  final String? siteName;
  final String? department;
  final String? service;
  final List<String> roles;
  final List<String> permissions;

  bool hasPermission(String code) => permissions.contains(code);

  factory UserSummary.fromJson(Map<String, dynamic> json) {
    return UserSummary(
      id: json['id'] as String,
      matricule: json['matricule'] as String,
      fullName: json['fullName'] as String,
      email: json['email'] as String,
      siteName: json['siteName'] as String?,
      department: json['department'] as String?,
      service: json['service'] as String?,
      roles: (json['roles'] as List<dynamic>? ?? const [])
          .map((e) => e as String)
          .toList(),
      permissions: (json['permissions'] as List<dynamic>? ?? const [])
          .map((e) => e as String)
          .toList(),
    );
  }
}
