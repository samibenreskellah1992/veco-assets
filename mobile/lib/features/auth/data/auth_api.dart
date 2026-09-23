/// Appels réseau bruts vers `/api/auth/...` (contrat vérifié directement
/// dans AuthController.java + LoginRequest/LoginResponse/UserSummaryDto —
/// voir docs/MOBILE-PHASE-0.md pour le reste de la cartographie API).
library;

import 'package:dio/dio.dart';

import '../../../core/errors/app_exception.dart';
import '../domain/user_summary.dart';
import 'login_result.dart';

class AuthApi {
  AuthApi(this._dio);

  final Dio _dio;

  Future<LoginResult> login({
    required String email,
    required String password,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '/api/auth/login',
        data: {'email': email, 'password': password},
      );
      return LoginResult.fromJson(response.data!);
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }

  Future<UserSummary> me() async {
    try {
      final response = await _dio.get<Map<String, dynamic>>('/api/auth/me');
      return UserSummary.fromJson(response.data!);
    } on DioException catch (e) {
      throw AppException.fromDio(e);
    }
  }
}
