package id.klaras.wifilogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.klaras.wifilogger.data.entity.WifiLog
import id.klaras.wifilogger.data.repository.WifiLogRepository
import id.klaras.wifilogger.wifi.ScanState
import id.klaras.wifilogger.wifi.WifiScanResult
import id.klaras.wifilogger.wifi.WifiScannerHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WifiScannerViewModel(
    private val wifiScannerHelper: WifiScannerHelper,
    private val wifiLogRepository: WifiLogRepository
) : ViewModel() {
    
    private val _scanState = MutableStateFlow<WifiScanUiState>(WifiScanUiState.Idle)
    val scanState: StateFlow<WifiScanUiState> = _scanState.asStateFlow()
    
    private val _scanResults = MutableStateFlow<List<WifiScanResult>>(emptyList())
    val scanResults: StateFlow<List<WifiScanResult>> = _scanResults.asStateFlow()
    
    private val _selectedNetworks = MutableStateFlow<Set<WifiScanResult>>(emptySet())
    val selectedNetworks: StateFlow<Set<WifiScanResult>> = _selectedNetworks.asStateFlow()
    
    private val _lastScanTime = MutableStateFlow<Long?>(null)
    val lastScanTime: StateFlow<Long?> = _lastScanTime.asStateFlow()
    
    fun startScan() {
        viewModelScope.launch {
            wifiScannerHelper.startScan().collect { state ->
                when (state) {
                    is ScanState.Scanning -> {
                        _scanState.value = WifiScanUiState.Scanning
                    }
                    is ScanState.WaitingForCooldown -> {
                        _scanState.value = WifiScanUiState.Cooldown(state.remainingMs)
                    }
                    is ScanState.Throttled -> {
                        _scanState.value = WifiScanUiState.Throttled(
                            state.message,
                            state.retryAfterMs
                        )
                    }
                    is ScanState.Success -> {
                        _scanResults.value = state.results
                        _lastScanTime.value = System.currentTimeMillis()
                        _scanState.value = WifiScanUiState.Success(
                            resultCount = state.results.size,
                            isCached = state.isCached
                        )
                    }
                    is ScanState.Error -> {
                        _scanState.value = WifiScanUiState.Error(state.message)
                    }
                }
            }
        }
    }
    
    fun getCachedResults() {
        val results = wifiScannerHelper.getScanResults()
        if (results.isNotEmpty()) {
            _scanResults.value = results
            _scanState.value = WifiScanUiState.Success(
                resultCount = results.size,
                isCached = true
            )
        }
    }
    
    fun toggleNetworkSelection(network: WifiScanResult) {
        val currentSelection = _selectedNetworks.value.toMutableSet()
        if (network in currentSelection) {
            currentSelection.remove(network)
        } else {
            currentSelection.add(network)
        }
        _selectedNetworks.value = currentSelection
    }
    
    fun selectAllNetworks() {
        _selectedNetworks.value = _scanResults.value.toSet()
    }
    
    fun clearSelection() {
        _selectedNetworks.value = emptySet()
    }
    
    fun saveSelectedNetworksToFloorPlan(
        floorPlanId: Long,
        coordinateX: Float,
        coordinateY: Float
    ) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val logs = _selectedNetworks.value.map { network ->
                WifiLog(
                    floorPlanId = floorPlanId,
                    coordinateX = coordinateX,
                    coordinateY = coordinateY,
                    ssid = network.ssid,
                    bssid = network.bssid,
                    rssi = network.rssi,
                    frequency = network.frequency,
                    timestamp = timestamp
                )
            }
            wifiLogRepository.insertAll(logs)
            clearSelection()
        }
    }
    
    fun saveAllResultsToFloorPlan(
        floorPlanId: Long,
        coordinateX: Float,
        coordinateY: Float
    ) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val logs = _scanResults.value.map { network ->
                WifiLog(
                    floorPlanId = floorPlanId,
                    coordinateX = coordinateX,
                    coordinateY = coordinateY,
                    ssid = network.ssid,
                    bssid = network.bssid,
                    rssi = network.rssi,
                    frequency = network.frequency,
                    timestamp = timestamp
                )
            }
            wifiLogRepository.insertAll(logs)
        }
    }
    
    fun isWifiEnabled(): Boolean = wifiScannerHelper.isWifiEnabled()
    
    fun getTimeUntilNextScan(): Long = wifiScannerHelper.getTimeUntilNextScan()
    
    fun isThrottled(): Boolean = wifiScannerHelper.isThrottled()
}

sealed class WifiScanUiState {
    data object Idle : WifiScanUiState()
    data object Scanning : WifiScanUiState()
    data class Cooldown(val remainingMs: Long) : WifiScanUiState()
    data class Throttled(val message: String, val retryAfterMs: Long) : WifiScanUiState()
    data class Success(val resultCount: Int, val isCached: Boolean) : WifiScanUiState()
    data class Error(val message: String) : WifiScanUiState()
}
