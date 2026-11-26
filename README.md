# Toasts Wifi Mapper

An offline-first Android Wi-Fi wardriving, GPS signal heatmap, indoor floor-plan survey, and channel congestion analyzer built with Kotlin, Jetpack Compose, Material 3, Room SQLite, and OpenStreetMap (`osmdroid`).

## Features

- **Offline-First & Private**: Zero cloud accounts, zero telemetry, zero ads, and no paid map API keys. All scan logs, GPS tracks, and floor-plan surveys live locally in Room SQLite. Only OpenStreetMap map tiles fetch over the network when online, with an offline grid fallback when disconnected.
- **Demo Mode on First Launch**: Preloads a ~26-AP neighborhood GPS walk (2.4 / 5 / 6 GHz) and a multi-room residential floor-plan survey comparing two router placements (`Router in Living Room` vs `Router in Central Hallway`) so you can explore heatmaps and inspector sheets right away. Includes top-bar toggles to reset demo data or wipe to a blank live database.
- **Outdoor GPS Map & Heatmap**:
  - OpenStreetMap (`osmdroid`) with pan, pinch-to-zoom, compass, live GPS track polyline, and toggleable color-coded RSSI heatmap (`-30..-60 dBm` Strong green, `-61..-75 dBm` Fair amber, `-76..-95 dBm` Weak crimson/violet).
  - Floating search bar and filter chips for Band (`All`, `2.4 GHz`, `5 GHz`, `6 GHz`), Security (`All`, `Open`, `WPA2`, `WPA3`), and single-BSSID isolation.
  - Scan cooldown badge tracking Android's foreground throttling budget (`4 scans / 2 min`) with guidance for Developer Options.
- **Indoor Floor-Plan & Router Placement**:
  - Use the built-in Craftsman house layout or import any floor plan via Android Photo Picker or Storage Access Framework (SAF).
  - Drop and drag virtual Router Pins, tap where you are standing to sample Wi-Fi RSSI, generate smooth Inverse Distance Weighting (IDW) heatmaps, and compare placement runs side-by-side with per-room `% Strong / % Fair / % Dead Zone` coverage breakdowns.
- **Wi-Fi Inspector Bottom Sheet**:
  - Tap any outdoor pin, indoor sample, or network row to inspect SSID/Hidden status, BSSID, offline OUI hardware manufacturer lookup, band, frequency (MHz), channel, channel width, Wi-Fi generation (4/5/6/6E/7), security protocol, peak/latest/average dBm, sightings count, and strongest coordinates.
- **Channel Analyzer & Exports**:
  - 2.4 GHz / 5 GHz / 6 GHz channel congestion bar chart accounting for channel bonding and 2.4 GHz overlap.
  - Export walks via SAF or Android Share Sheet as **CSV**, **GeoJSON** (QGIS/GIS ready), or **WiGLE CSV (v1.6)**.

## Usage

### Outdoor Mapping
1. Open the **Outdoor** tab. In Demo Mode you can immediately pan, toggle the heatmap, filter by band/security, and tap AP markers.
2. Switch **Demo** off (or tap **Start Mapping**) and grant `ACCESS_FINE_LOCATION` and `NEARBY_WIFI_DEVICES`.
3. Walk or drive while the app logs GPS fixes and Wi-Fi scan batches to Room.

### Indoor Router Placement Survey
1. Open the **Indoor** tab and pick the built-in floor plan or import your own image (**Photo** or folder icon).
2. Select **Place Pin** to drop/drag a virtual router marker, then switch to **Sample** and tap the floor plan where you are standing to capture RSSI readings.
3. Create a second run (e.g., *Hallway* vs *Living Room*) and tap **Compare** to inspect room-by-room signal coverage side-by-side.

## Android Wi-Fi Scan Throttling Notes

Since Android 9 (API 28), foreground apps are limited by the OS to **4 active `WifiManager.startScan()` calls per 2 minutes**. When the cooldown budget is reached, Toasts Wifi Mapper keeps logging your GPS track and falls back cleanly to the latest system scan cache.

For faster sweeps on a dedicated survey device:
1. Enable **Developer options** on your Android device.
2. Open **Settings → System → Developer options**.
3. Turn **OFF** **Wi-Fi scan throttling**.

## Architecture & KMP Readiness

Core domain models, OUI lookup (`OuiLookup`), RF/IDW/channel math (`SignalMath`), export formatters (`ExportFormatter`), and repository/scanner/location ports (`WifiRepository`, `WifiScannerPort`, `LocationPort`) live under `com.frenchtoast.wifimapper.domain` with no Android UI dependencies so business logic can be extracted into a Kotlin Multiplatform `commonMain` module later.

## Building with Gradle

```bash
gradle assembleDebug
gradle :app:testDebugUnitTest
```

*(Note: AI Studio's `debugConfig` signing configuration is retained in `app/build.gradle.kts` for the container build environment.)*

## Roadmap

- Google Play Store release track polish & offline MBTiles / vector tile pack import
- Multi-floor elevation tagging for multi-story indoor surveys
- Kotlin / Compose Multiplatform iOS port (with CoreLocation & Apple `NEHotspotHelper` / single-SSID adapter constraints documented in-app)
