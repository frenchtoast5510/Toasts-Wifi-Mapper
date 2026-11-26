# Toasts Wifi Mapper

Offline-first Android Wi-Fi wardriving, GPS signal heatmap, indoor floor-plan survey, and channel congestion analyzer built with Kotlin, Jetpack Compose, Room, and OpenStreetMap (`osmdroid`).

## Features

- **Offline-First & Private**: No cloud accounts, analytics, telemetry, or ads. All scan logs, GPS tracks, and floor-plan surveys are stored locally in Room SQLite. Only OpenStreetMap map tiles fetch over the network when online, falling back to a local grid when disconnected.
- **Demo Mode on First Launch**: Preloads a ~26-AP neighborhood GPS walk (2.4 / 5 / 6 GHz) and a residential floor-plan survey comparing two router placements (`Router in Living Room` vs `Router in Central Hallway`). Toggle Demo Mode, reset sample data, or wipe to a blank live database from the top-right overflow menu.
- **Outdoor GPS Map & Heatmap**:
  - Dark-styled OpenStreetMap (`osmdroid`) map with pan, pinch-to-zoom, compass, GPS track polyline, and toggleable RSSI heatmap (`Great`, `Okay`, `Weak`).
  - Floating search bar and filter chips for Band (`All`, `2.4 GHz`, `5 GHz`, `6 GHz`), Security (`All`, `Open`, `WPA2`, `WPA3`), and single-BSSID isolation.
  - Scan cooldown badge tracking Android's foreground throttling budget (`4 scans / 2 min`) with instructions for Developer Options.
- **Indoor Floor-Plan & Router Placement**:
  - Use the built-in house blueprint or import a floor plan via Photo Picker or Storage Access Framework (SAF).
  - Drop and drag virtual router pins, tap where you are standing to sample Wi-Fi RSSI, generate smooth IDW heatmaps, and compare placement runs side-by-side with per-room coverage breakdowns.
- **Wi-Fi Inspector Bottom Sheet**:
  - Tap any outdoor pin, indoor sample, or network row to inspect SSID/Hidden status, BSSID, offline OUI vendor lookup, band, frequency (MHz), channel, width, Wi-Fi generation (4/5/6/6E/7), security protocol, peak/latest/average dBm, sightings count, and strongest coordinates.
- **Channel Analyzer & Exports**:
  - 2.4 GHz / 5 GHz / 6 GHz channel congestion bar chart accounting for channel bonding and 2.4 GHz overlap.
  - Export walks via SAF or Android Share Sheet as **CSV**, **GeoJSON** (QGIS/GIS ready), or **WiGLE CSV (v1.6)**.

## Usage

### Outdoor Mapping
1. Open the **Outdoor** tab. In Demo Mode you can pan, toggle the heatmap, filter by band or security, and tap AP markers right away.
2. Open the overflow menu, turn **Demo Mode** off (or tap **Start Mapping**), and grant `ACCESS_FINE_LOCATION` and `NEARBY_WIFI_DEVICES`.
3. Walk or drive while the app logs GPS fixes and Wi-Fi scan batches to Room.

### Indoor Router Placement Survey
1. Open the **Indoor** tab and use the built-in floor plan or import your own image (**Photo** or folder icon).
2. Choose **Place Pin** to drop or drag a virtual router marker, then choose **Sample** and tap the floor plan where you are standing to capture RSSI readings.
3. Create a second run (e.g., *Hallway* vs *Living Room*) and tap **Compare** to view room-by-room signal coverage side-by-side.

## Android Wi-Fi Scan Throttling Notes

Since Android 9 (API 28), foreground apps are limited by the OS to **4 active `WifiManager.startScan()` calls per 2 minutes**. When the budget is reached, Toasts Wifi Mapper keeps logging your GPS track and falls back to the latest system scan cache.

For faster sweeps on a dedicated survey device:
1. Enable **Developer options** on your Android device.
2. Open **Settings → System → Developer options**.
3. Turn **OFF** **Wi-Fi scan throttling**.

## Architecture

Domain models, OUI lookup (`OuiLookup`), RF/IDW/channel math (`SignalMath`), export formatters (`ExportFormatter`), and repository/scanner/location ports (`WifiRepository`, `WifiScannerPort`, `LocationPort`) live under `com.frenchtoast.wifimapper.domain` with no Android UI dependencies so business logic can later move into a Kotlin Multiplatform `commonMain` module.

## Building

```bash
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest
```

## Roadmap

- Play Store release polish and offline MBTiles / vector tile pack import
- Multi-floor elevation tagging for multi-story indoor surveys
- Kotlin / Compose Multiplatform iOS port (subject to CoreLocation and `NEHotspotHelper` constraints)
