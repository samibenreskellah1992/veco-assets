// Test de fumée : sans session stockée, l'app doit passer du Splash à
// l'écran de connexion. Le TokenStorage réel (flutter_secure_storage)
// passe par un canal de plateforme indisponible en test widget : on le
// remplace par un faux qui ne stocke jamais rien, pour ne tester que la
// navigation, pas le plugin natif.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/storage/token_storage.dart';
import 'package:mobile/features/auth/presentation/auth_providers.dart';
import 'package:mobile/main.dart';

class _FakeTokenStorage implements TokenStorage {
  @override
  Future<void> saveToken(String token) async {}

  @override
  Future<String?> readToken() async => null;

  @override
  Future<void> clear() async {}
}

void main() {
  testWidgets('No stored session -> app lands on the login screen',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          tokenStorageProvider.overrideWithValue(_FakeTokenStorage()),
        ],
        child: const VecoAssetsMobileApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Connexion'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
  });
}
