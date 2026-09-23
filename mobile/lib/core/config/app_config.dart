/// Configuration d'environnement VECO Assets Mobile.
///
/// [À CONFIRMER PAR SAMI] : l'adresse exacte pour joindre le backend depuis
/// l'appareil Android dépend de comment le backend tourne (docker compose,
/// port 8080 confirmé dans docker-compose.yml) et de comment l'app est
/// testée :
///   - Émulateur Android : `10.0.2.2` pointe vers le `localhost` de la
///     machine hôte -> http://10.0.2.2:8080
///   - Téléphone physique en USB (cas de Sami, pas d'émulateur configuré) :
///     le plus simple sans toucher au réseau Wi-Fi est de rediriger le
///     port du téléphone vers celui de la machine avec
///     `adb reverse tcp:8080 tcp:8080`, puis d'utiliser
///     http://127.0.0.1:8080 (comme si le backend tournait sur le
///     téléphone lui-même).
///   - Téléphone physique sur le même Wi-Fi que la machine : adresse LAN de
///     la machine (ex. http://192.168.1.x:8080).
/// Valeur par défaut ci-dessous = hypothèse `adb reverse`. Sami : dis-moi
/// laquelle des trois tu utilises et j'ajuste (ou passe-la en variable
/// d'environnement de build avec --dart-define si tu préfères basculer
/// facilement).
library;

abstract final class AppConfig {
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://127.0.0.1:8080',
  );

  static const Duration apiTimeout = Duration(seconds: 15);
}
