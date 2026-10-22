# Changelog

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
