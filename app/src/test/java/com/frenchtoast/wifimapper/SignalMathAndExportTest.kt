package com.frenchtoast.wifimapper

import com.frenchtoast.wifimapper.data.demo.DemoDatasetFactory
import com.frenchtoast.wifimapper.domain.math.SignalMath
import com.frenchtoast.wifimapper.domain.model.OuiLookup
import com.frenchtoast.wifimapper.domain.model.WifiBand
import com.frenchtoast.wifimapper.domain.model.WifiSecurity
import com.frenchtoast.wifimapper.domain.repository.ExportFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalMathAndExportTest {

  @Test
  fun frequencyMappingCoversAllThreeBands() {
    assertEquals(WifiBand.BAND_2_4_GHZ to 1, SignalMath.frequencyToBandAndChannel(2412))
    assertEquals(WifiBand.BAND_2_4_GHZ to 6, SignalMath.frequencyToBandAndChannel(2437))
    assertEquals(WifiBand.BAND_5_GHZ to 36, SignalMath.frequencyToBandAndChannel(5180))
    assertEquals(WifiBand.BAND_6_GHZ to 33, SignalMath.frequencyToBandAndChannel(6115))
  }

  @Test
  fun ouiLookupDetectsKnownAndRandomizedMacs() {
    assertEquals("Ubiquiti Inc.", OuiLookup.lookup("24:5A:4C:11:2A:01"))
    assertEquals("Cisco Meraki", OuiLookup.lookup("00:18:0A:42:99:10"))
    assertEquals("Randomized / Local MAC", OuiLookup.lookup("02:11:22:33:44:55"))
  }

  @Test
  fun demoDataProducesExpectedOutdoorApsAndBetterHallwayCoverage() {
    val readings = DemoDatasetFactory.buildOutdoorReadings(1L, 1_000_000L)
    val summaries = SignalMath.summarizeAccessPoints(readings)
    assertTrue("Expected ~25+ demo APs, got ${summaries.size}", summaries.size >= 24)

    val (_, livingSamples) = DemoDatasetFactory.buildIndoorRunLivingRoom(1L, 1_000_000L)
    val (_, hallwaySamples) = DemoDatasetFactory.buildIndoorRunHallway(2L, 1_000_000L)

    val livingOverall = SignalMath.computeOverallCoverage(
      SignalMath.computeRoomCoverage(livingSamples, SignalMath.defaultDemoRooms),
    )
    val hallwayOverall = SignalMath.computeOverallCoverage(
      SignalMath.computeRoomCoverage(hallwaySamples, SignalMath.defaultDemoRooms),
    )
    assertTrue(
      "Central hallway should yield higher overall strong coverage than corner living room",
      hallwayOverall.strongPercent > livingOverall.strongPercent,
    )
  }

  @Test
  fun exportFormattersProduceValidHeaders() {
    val readings = DemoDatasetFactory.buildOutdoorReadings(1L, 1_700_000_000_000L).take(5)
    val track = DemoDatasetFactory.buildOutdoorTrack(1L, 1_700_000_000_000L)
    val summaries = SignalMath.summarizeAccessPoints(readings)

    val csv = ExportFormatter.toCsv(readings)
    assertTrue(csv.startsWith("bssid,ssid,rssi_dbm"))

    val wigle = ExportFormatter.toWigleCsv(readings, "Pixel 9")
    assertTrue(wigle.startsWith("WigleWifi-1.6"))
    assertTrue(wigle.contains("MAC,SSID,AuthMode"))

    val geojson = ExportFormatter.toGeoJson(summaries, track, "Demo Walk")
    assertTrue(geojson.contains("\"FeatureCollection\""))
    assertTrue(geojson.contains("\"LineString\""))
  }

  @Test
  fun securityParserHandlesWpa3Wpa2EnterpriseOpen() {
    assertEquals(WifiSecurity.WPA3, SignalMath.parseSecurity("[WPA3-SAE-CCMP][ESS]"))
    assertEquals(WifiSecurity.WPA2_PSK, SignalMath.parseSecurity("[WPA2-PSK-CCMP][ESS]"))
    assertEquals(WifiSecurity.ENTERPRISE, SignalMath.parseSecurity("[WPA2-EAP-CCMP][ESS]"))
    assertEquals(WifiSecurity.OPEN, SignalMath.parseSecurity("[ESS]"))
  }
}
