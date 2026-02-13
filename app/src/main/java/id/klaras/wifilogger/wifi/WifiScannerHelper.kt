package id.klaras.wifilogger.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Helper class to handle WiFi scanning operations.
 * 
 * Note: Android throttles WiFi scans:
 * - Android 8+: Each foreground app can scan 4 times in 2 minutes
 * - Android 9+: Each background app can scan 1 time in 30 minutes
 */
class WifiScannerHelper(private val context: Context) {
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    companion object {
        // Minimum interval between scans to avoid throttling (30 seconds)
        const val MIN_SCAN_INTERVAL_MS = 30_000L
        
        // Scan attempts before showing throttle warning
        const val THROTTLE_WARNING_AFTER_SCANS = 4
    }
    
    private var lastScanTime: Long = 0
    private var scanCountInWindow: Int = 0
    private var windowStartTime: Long = 0
    
    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }
    
    /**
     * Get time remaining until next scan is allowed (to avoid throttling)
     */
    fun getTimeUntilNextScan(): Long {
        val elapsed = System.currentTimeMillis() - lastScanTime
        val remaining = MIN_SCAN_INTERVAL_MS - elapsed
        return if (remaining > 0) remaining else 0
    }
    
    /**
     * Check if scanning might be throttled
     */
    fun isThrottled(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Reset counter if window has passed (2 minutes)
        if (currentTime - windowStartTime > 120_000) {
            scanCountInWindow = 0
            windowStartTime = currentTime
        }
        
        return scanCountInWindow >= THROTTLE_WARNING_AFTER_SCANS
    }
    
    /**
     * Start a WiFi scan and return results as a Flow.
     * This handles the broadcast receiver registration and cleanup.
     */
    fun startScan(): Flow<ScanState> = callbackFlow {
        val currentTime = System.currentTimeMillis()

        // Check for throttling
        if (isThrottled()) {
            trySend(ScanState.Throttled(
                message = "Too many scan requests. Android limits scans to 4 per 2 minutes.",
                retryAfterMs = 120_000 - (currentTime - windowStartTime)
            ))
            close()
            return@callbackFlow
        }

        // Check if enough time has passed since last scan
        val timeUntilNextScan = getTimeUntilNextScan()
        if (timeUntilNextScan > 0) {
            trySend(ScanState.WaitingForCooldown(timeUntilNextScan))
            close()
            return@callbackFlow
        }

        // Check if WiFi is enabled
        if (!isWifiEnabled()) {
            trySend(ScanState.Error("WiFi is disabled. Please enable WiFi to scan."))
            close()
            return@callbackFlow
        }

        trySend(ScanState.Scanning)

        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                } else {
                    true
                }
                
                if (success) {
                    val results = getScanResults()
                    trySend(ScanState.Success(results))
                } else {
                    // Use cached results if scan failed
                    val cachedResults = getScanResults()
                    if (cachedResults.isNotEmpty()) {
                        trySend(ScanState.Success(cachedResults, isCached = true))
                    } else {
                        trySend(ScanState.Error("Scan failed. Possible throttling by the system."))
                    }
                }
                close()
            }
        }
        
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(wifiScanReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(wifiScanReceiver, intentFilter)
        }

        // Start the scan
        val scanStarted = wifiManager.startScan()

        if (!scanStarted) {
            // Scan couldn't be started, return cached results
            val cachedResults = getScanResults()
            if (cachedResults.isNotEmpty()) {
                trySend(ScanState.Success(cachedResults, isCached = true))
            } else {
                trySend(ScanState.Error("Could not start scan. System may be throttling requests."))
            }
        }

        // Update tracking
        lastScanTime = currentTime
        scanCountInWindow++
        if (scanCountInWindow == 1) {
            windowStartTime = currentTime
        }

        awaitClose {
            try {
                context.unregisterReceiver(wifiScanReceiver)
            } catch (e: Exception) {
                // Receiver might already be unregistered
            }
        }
    }
    
    /**
     * Get the current scan results (may be cached by the system)
     */
    fun getScanResults(): List<WifiScanResult> {
        return try {
            wifiManager.scanResults
                .map { WifiScanResult.fromScanResult(it) }
                .sortedByDescending { it.rssi }
        } catch (e: SecurityException) {
            emptyList()
        }
    }
    
    /**
     * Get scan results grouped by SSID (taking the strongest signal for duplicates)
     */
    fun getScanResultsGroupedBySsid(): List<WifiScanResult> {
        return getScanResults()
            .groupBy { it.ssid }
            .map { (_, results) -> results.maxByOrNull { it.rssi }!! }
            .sortedByDescending { it.rssi }
    }
}

/**
 * Sealed class representing the state of a WiFi scan operation
 */
sealed class ScanState {
    data object Scanning : ScanState()
    data class WaitingForCooldown(val remainingMs: Long) : ScanState()
    data class Throttled(val message: String, val retryAfterMs: Long) : ScanState()
    data class Success(val results: List<WifiScanResult>, val isCached: Boolean = false) : ScanState()
    data class Error(val message: String) : ScanState()
}
