// Test de fumée : vérifie que l'application démarre sur le tableau de
// bord et que la barre de navigation à 4 onglets est bien affichée.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/main.dart';

void main() {
  testWidgets('App starts on the dashboard with bottom navigation',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: VecoAssetsMobileApp()),
    );
    await tester.pumpAndSettle();

    expect(find.text('Tableau de bord'), findsWidgets);
    expect(find.byType(NavigationBar), findsOneWidget);
  });
}
