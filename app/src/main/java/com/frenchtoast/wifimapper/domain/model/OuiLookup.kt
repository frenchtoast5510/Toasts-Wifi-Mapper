package com.frenchtoast.wifimapper.domain.model

object OuiLookup {
  private val vendorByPrefix: Map<String, String> = mapOf(
    "24:5A:4C" to "Ubiquiti Inc.",
    "78:8A:20" to "Ubiquiti Inc.",
    "F0:9F:C2" to "Ubiquiti Inc.",
    "74:83:C2" to "Ubiquiti Inc.",
    "68:D7:9A" to "Ubiquiti Inc.",
    "B4:FB:E4" to "Ubiquiti Inc.",
    "00:18:0A" to "Cisco Meraki",
    "E0:55:3D" to "Cisco Meraki",
    "88:15:44" to "Cisco Meraki",
    "0C:8D:DB" to "Cisco Meraki",
    "00:1A:1E" to "Aruba (HPE)",
    "24:DE:C6" to "Aruba (HPE)",
    "94:B4:0F" to "Aruba (HPE)",
    "B0:B9:8A" to "Netgear",
    "28:C6:8E" to "Netgear",
    "A0:04:60" to "Netgear",
    "9C:3D:CF" to "Netgear",
    "50:C7:BF" to "TP-Link",
    "B0:95:75" to "TP-Link",
    "D8:47:32" to "TP-Link",
    "3C:84:6A" to "TP-Link",
    "1C:61:B4" to "TP-Link",
    "04:D4:C4" to "ASUS",
    "2C:FD:A1" to "ASUS",
    "70:4D:7B" to "ASUS",
    "AC:9E:17" to "ASUS",
    "F4:F5:E8" to "Google Nest",
    "54:60:09" to "Google Nest",
    "D8:6C:63" to "Google Nest",
    "48:D6:D5" to "Google Nest",
    "70:3A:CB" to "Google",
    "3C:5A:B4" to "Google",
    "B4:75:0E" to "Linksys / Belkin",
    "C0:56:27" to "Linksys / Belkin",
    "14:91:82" to "Linksys / Belkin",
    "44:D9:E7" to "Ubiquiti UniFi",
    "80:2A:A8" to "Ubiquiti UniFi",
    "DC:A6:32" to "Raspberry Pi",
    "B8:27:EB" to "Raspberry Pi",
    "E4:5F:01" to "Raspberry Pi",
    "F0:27:2D" to "Amazon / eero",
    "FC:65:DE" to "Amazon / eero",
    "78:E1:03" to "Amazon / eero",
    "34:D2:70" to "eero Inc.",
    "CC:F7:35" to "eero Inc.",
    "20:C0:47" to "ARRIS / Comcast",
    "E4:9E:12" to "ARRIS / Xfinity",
    "98:9B:CB" to "AVM FRITZ!Box",
    "24:65:11" to "AVM FRITZ!Box",
    "00:11:32" to "Synology",
    "90:09:D0" to "Synology",
    "D4:CA:6D" to "MikroTik",
    "4C:5E:0C" to "MikroTik",
    "64:D1:54" to "MikroTik",
    "00:25:9C" to "Cisco-Linksys",
    "A4:2B:B0" to "TP-Link",
    "00:0C:43" to "Ralink / MediaTek",
    "C8:3A:35" to "Tenda",
    "78:D2:94" to "Netgear Orbi",
    "B8:27:8E" to "Sonos",
    "94:9F:3E" to "Sonos",
    "28:6D:97" to "Samsung",
    "A8:66:7F" to "Apple",
    "F0:18:98" to "Apple",
    "3C:22:FB" to "Apple",
    "24:0A:C4" to "Espressif IoT",
    "30:AE:A4" to "Espressif IoT",
    "C4:4F:33" to "Espressif IoT",
    "AC:22:0B" to "Ruckus Wireless",
    "58:93:96" to "Ruckus Wireless",
    "D0:73:D5" to "LIFX IoT",
  )

  fun lookup(bssid: String): String {
    val normalized = bssid.uppercase().replace('-', ':').trim()
    if (normalized.length < 8) return "Unknown Vendor"
    val prefix = normalized.substring(0, 8)
    vendorByPrefix[prefix]?.let { return it }

    val firstOctet = normalized.substring(0, 2).toIntOrNull(16)
    if (firstOctet != null && (firstOctet and 0x02) != 0) {
      return "Randomized / Local MAC"
    }
    return "Unknown Vendor ($prefix)"
  }
}
