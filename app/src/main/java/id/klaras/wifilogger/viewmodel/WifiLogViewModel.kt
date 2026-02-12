package id.klaras.wifilogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.klaras.wifilogger.data.dao.ScanPointSummary
import id.klaras.wifilogger.data.entity.WifiLog
import id.klaras.wifilogger.data.repository.WifiLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WifiLogViewModel(
    private val repository: WifiLogRepository
) : ViewModel() {
    
    val wifiLogs: StateFlow<List<WifiLog>> = repository.getAllWifiLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _logsForFloorPlan = MutableStateFlow<List<WifiLog>>(emptyList())
    val logsForFloorPlan: StateFlow<List<WifiLog>> = _logsForFloorPlan.asStateFlow()
    
    private val _scanPoints = MutableStateFlow<List<ScanPointSummary>>(emptyList())
    val scanPoints: StateFlow<List<ScanPointSummary>> = _scanPoints.asStateFlow()
    
    private val _selectedWifiLog = MutableStateFlow<WifiLog?>(null)
    val selectedWifiLog: StateFlow<WifiLog?> = _selectedWifiLog.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun selectWifiLog(wifiLog: WifiLog?) {
        _selectedWifiLog.value = wifiLog
    }
    
    fun loadLogsForFloorPlan(floorPlanId: Long) {
        viewModelScope.launch {
            repository.getLogsByFloorPlanId(floorPlanId).collect { logs ->
                _logsForFloorPlan.value = logs
            }
        }
    }
    
    fun loadScanPoints(floorPlanId: Long) {
        viewModelScope.launch {
            try {
                _scanPoints.value = repository.getDistinctScanPoints(floorPlanId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    fun loadWifiLogById(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _selectedWifiLog.value = repository.getWifiLogByIdOnce(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun insertWifiLog(
        floorPlanId: Long,
        coordinateX: Float,
        coordinateY: Float,
        ssid: String,
        bssid: String,
        rssi: Int,
        frequency: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val wifiLog = WifiLog(
                    floorPlanId = floorPlanId,
                    coordinateX = coordinateX,
                    coordinateY = coordinateY,
                    ssid = ssid,
                    bssid = bssid,
                    rssi = rssi,
                    frequency = frequency,
                    timestamp = timestamp
                )
                repository.insert(wifiLog)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun insertWifiLogs(wifiLogs: List<WifiLog>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.insertAll(wifiLogs)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateWifiLog(wifiLog: WifiLog) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.update(wifiLog)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteWifiLog(wifiLog: WifiLog) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.delete(wifiLog)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteWifiLogById(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteById(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteLogsForFloorPlan(floorPlanId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteByFloorPlanId(floorPlanId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
