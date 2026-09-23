/// Exception applicative unifiée : traduit une [DioException] (ou toute
/// autre erreur) en message lisible pour l'utilisateur, sans exposer les
/// détails techniques dans l'UI.
library;

import 'package:dio/dio.dart';

class AppException implements Exception {
  const AppException(this.message);

  final String message;

  factory AppException.fromDio(DioException e) {
    final status = e.response?.statusCode;
    final data = e.response?.data;

    // Le backend Spring renvoie généralement {"message": "..."} ou
    // {"error": "..."} sur les erreurs métier/validation (à ajuster si le
    // format réel observé diffère une fois testé par Sami).
    if (data is Map && data['message'] is String) {
      return AppException(data['message'] as String);
    }

    switch (status) {
      case 401:
        return const AppException('Email ou mot de passe incorrect.');
      case 403:
        return const AppException('Accès refusé pour ce compte.');
      case null:
        return const AppException(
          "Impossible de joindre le serveur. Vérifiez votre connexion.",
        );
      default:
        return AppException('Erreur serveur ($status). Réessayez.');
    }
  }

  @override
  String toString() => message;
}
