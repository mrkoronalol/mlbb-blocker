# Changelog

## 0.19.0

- Anonymous telemetry: a random device id, events queued in Room and sent in batches every 15 minutes.
- Funnel events: `app_open`, `vpn_enabled`, `meta_opened`, `profile_selected`, `config_sync`.
- Blocked/pinged server IPs are reported (hashed on the server) to build the server map.
- Remote config from the site: maintenance mode, Meta tab switch, global notice, minimal app version.
- Config is synced every 6 hours and on launch; on failure the last saved config is used, the app never crashes.
- Profiles are now served by remote config with a fallback to the bundled rules.
- Update-required dialog when the installed version is below the configured minimum.

## 0.18.0

- Monitor screen: compact rows (IP + city + ping), actions moved to a bottom sheet.
- Monitor header shows count and best ping; export/clear moved into the overflow menu.
- About screen rebuilt with expandable sections; release history collapsed.
- Less on-screen text across screens; empty states and descriptions live in sheets.

## 0.17.0

- Compact home: three profile buttons in a row, descriptions moved to an info bottom sheet.
- Primary enable/disable button moved above profiles and fits small screens without scrolling.
- Blocklist rows are compact (monospace IP + region badge, no long notes); swipe left to delete.
- Settings reorganized into expandable sections; rules row is now a single line with a refresh icon.
- Meta shows a friendly cloud empty state instead of raw API errors; hero tiles enlarged to 72dp.
- Match screen no longer duplicates the profile picker.

## 0.16.0

- Redesigned UI to match the mlbb-meta site: navy `#0B0F19`, cards `#14182A` with `#2A3756` borders, blue/cyan accents.
- Six bottom tabs: Home, Match, Monitor, Blocklist, Meta, Settings.
- New Meta tab: S/A/B/C/D tiers, heroes grouped by lane, hero sheet with win/pick/ban rates.
- Meta data comes from the site (`GET /api/v1/app/meta`) and is cached in Room for offline use.
- Region selector (World / Moscow / Far East) and a local counterpick ban list.
- API base URL is configurable in the app; default is the `https://your-domain.com` stub.

## 0.15.0

- Match card shows city and country, profile, and full ping (average, jitter, loss).
- «Заблокировать» and «Не показывать» buttons on the card; block from the notification too.
- New setting «Только стабильные соединения»: allowed servers show a card only after 6 seconds.
- Blocked servers always show the card regardless of filters.
- Match card decision logic moved to `MatchCardPolicy` and covered by unit tests.

## 0.14.0

- VPN автоматически поднимается после обновления APK (`MY_PACKAGE_REPLACED`).
- Если поднять не удалось — уведомление «Блокировка остановлена» с кнопкой «Включить».
- Health-check на главной: сервис остановлен, GeoIP не открывается, правила не загрузились.
- Одна автоматическая повторная попытка запуска при падении, затем уведомление вместо тихого выхода.
- Правила вынесены в `rules.json`: матч-порты, диапазоны, сервисные порты, стартовые серверы, профили, известные серверы.
- `rules.json` подписывается release-ключом и публикуется в GitHub-релизе; приложение проверяет подпись сертификатом APK.
- Ежедневная проверка правил через WorkManager; при ошибке остаётся последний рабочий набор.
- Уведомление о незнакомом порте с выбором «матч-порт / сервисный / игнорировать».
- Новые unit-тесты: парсер правил, проверка подписи, health-check.

## 0.13.3

- Fixed bundled GeoIP being stripped by R8/ProGuard in release builds.
- Added observed match/relay ports 10011-10013, 10063 and 5500-5599.
- Normalized city names with district suffixes.

## 0.13.2

- Added a switch to allow or block the main connection server `154.85.91.151:10003`.
- When disabled, this Moscow server is filtered and shows the match card.
- Blocked match servers always show the card.

## 0.13.1

- Blocked match servers always show the match card; liveness filtering now applies only to allowed connections.
- Fixed missing notifications for some Moscow IPs.

## 0.13.0

- JSON backup/export/import for settings, profiles, rules, allowlist and history.
- Automatic local backup before downloading an update.
- In-app APK download with SHA256 and signature verification.
- PackageInstaller update flow with unknown-sources handling.
- Backup/update cards in Settings.

## 0.12.0

- Signed release APK and automated GitHub Actions release pipeline.
- Smarter match alerts: short startup connections are filtered by a 2.5-second liveness check.
- Toggle for hiding startup match cards.
- Profiles now block only match ports; service ports 30021, 30071, 30190 and 30220 stay direct.
- Main connection server 154.85.91.151:10003 is always allowed and never shows a card.
- Monitor: search, sorting, dynamic country filters, best-ping card.
- Region-neutral blocklist presets: Moscow, Novosibirsk, non-Far-East.
- Removed the PCAP block from Settings.
- HTTPS-only network security config; local Clash API remains allowed.
- Added GPL-3.0 license and release checksums.

## 0.11.4

- Skip the first match card after starting the VPN.

## 0.11.3

- Removed the 60-second startup grace timer.

## 0.11.2

- Temporary startup grace and port 30190 bypass.

## 0.11.1

- MB circular adaptive icon and monochrome icon.

## 0.11.0

- Compact match card, region-aware Monitor, GitHub update checks.

## 0.10.0

- Match card overlay with city, ping and allow action.

## 0.9.0

- Bundled GeoIP and Moscow/Novosibirsk allow-only profiles.

## 0.8.0

- Room monitoring, blocked attempts, allowlist and notification actions.
