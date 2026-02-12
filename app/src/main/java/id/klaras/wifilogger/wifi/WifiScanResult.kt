package id.klaras.wifilogger.wifi

import android.net.wifi.ScanResult

/**
 * Data class representing a WiFi network scan result with essential information
 */
data class WifiScanResult(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String
) {
    companion object {
        fun fromScanResult(scanResult: ScanResult): WifiScanResult {
            return WifiScanResult(
                ssid = scanResult.SSID.takeIf { it.isNotEmpty() } ?: "<Hidden Network>",
                bssid = scanResult.BSSID,
                rssi = scanResult.level,
                frequency = scanResult.frequency,
                capabilities = scanResult.capabilities
            )
        }
    }
    
    /**
     * Returns signal strength as a percentage (0-100)
     */
    fun getSignalStrengthPercent(): Int {
        return when {
            rssi >= -50 -> 100
            rssi >= -60 -> 80
            rssi >= -70 -> 60
            rssi >= -80 -> 40
            rssi >= -90 -> 20
            else -> 0
        }
    }
    
    /**
     * Returns a human-readable signal quality description
     */
    fun getSignalQuality(): String {
        return when {
            rssi >= -50 -> "Excellent"
            rssi >= -60 -> "Good"
            rssi >= -70 -> "Fair"
            rssi >= -80 -> "Weak"
            else -> "Very Weak"
        }
    }
    
    /**
     * Returns the WiFi band (2.4 GHz or 5 GHz)
     */
    fun getBand(): String {
        return if (frequency < 3000) "2.4 GHz" else "5 GHz"
    }
}
