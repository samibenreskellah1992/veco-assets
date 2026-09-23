/// Client HTTP unique de l'application (Dio), partagé par tous les modules.
///
/// Attache automatiquement le token JWT (si présent) à chaque requête via
/// un [Interceptor] — les futurs appels API (Locaux, Immobilisations,
/// Mouvements...) n'ont pas à s'en soucier.
library;

import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../storage/token_storage.dart';

class DioClient {
  DioClient({TokenStorage? tokenStorage})
      : _tokenStorage = tokenStorage ?? TokenStorage() {
    dio = Dio(
      BaseOptions(
        baseUrl: AppConfig.apiBaseUrl,
        connectTimeout: AppConfig.apiTimeout,
        receiveTimeout: AppConfig.apiTimeout,
        headers: const {'Content-Type': 'application/json'},
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _tokenStorage.readToken();
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
      ),
    );
  }

  final TokenStorage _tokenStorage;
  late final Dio dio;
}
