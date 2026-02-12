package id.klaras.wifilogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.klaras.wifilogger.data.dao.HeatmapPoint
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.data.repository.FloorPlanRepository
import id.klaras.wifilogger.data.repository.WifiLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HeatmapUiState {
    data object Loading : HeatmapUiState()
    data class Success(
        val floorPlan: FloorPlan,
        val heatmapPoints: List<HeatmapPoint>,
        val availableSsids: List<String>,
        val selectedSsid: String? // null means "All Networks"
    ) : HeatmapUiState()
    data class Error(val message: String) : HeatmapUiState()
    data object NoData : HeatmapUiState()
}

class HeatmapViewModel(
    private val floorPlanRepository: FloorPlanRepository,
    private val wifiLogRepository: WifiLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HeatmapUiState>(HeatmapUiState.Loading)
    val uiState: StateFlow<HeatmapUiState> = _uiState.asStateFlow()

    private var currentFloorPlanId: Long = -1
    private var currentFloorPlan: FloorPlan? = null
    private var availableSsids: List<String> = emptyList()

    fun loadHeatmap(floorPlanIdString: String) {
        val floorPlanId = floorPlanIdString.toLongOrNull()
        if (floorPlanId == null) {
            _uiState.value = HeatmapUiState.Error("Invalid floor plan ID")
            return
        }

        currentFloorPlanId = floorPlanId
        _uiState.value = HeatmapUiState.Loading

        viewModelScope.launch {
            try {
                val floorPlan = floorPlanRepository.getFloorPlanByIdOnce(floorPlanId)
                if (floorPlan == null) {
                    _uiState.value = HeatmapUiState.Error("Floor plan not found")
                    return@launch
                }

                currentFloorPlan = floorPlan
                availableSsids = wifiLogRepository.getDistinctSsidsForFloorPlan(floorPlanId)
                val heatmapPoints = wifiLogRepository.getHeatmapPoints(floorPlanId)

                if (heatmapPoints.isEmpty()) {
                    _uiState.value = HeatmapUiState.NoData
                } else {
                    _uiState.value = HeatmapUiState.Success(
                        floorPlan = floorPlan,
                        heatmapPoints = heatmapPoints,
                        availableSsids = availableSsids,
                        selectedSsid = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HeatmapUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun selectSsid(ssid: String?) {
        val floorPlan = currentFloorPlan ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = HeatmapUiState.Loading
                
                val heatmapPoints = if (ssid == null) {
                    wifiLogRepository.getHeatmapPoints(currentFloorPlanId)
                } else {
                    wifiLogRepository.getHeatmapPointsForSsid(currentFloorPlanId, ssid)
                }

                if (heatmapPoints.isEmpty()) {
                    _uiState.value = HeatmapUiState.NoData
                } else {
                    _uiState.value = HeatmapUiState.Success(
                        floorPlan = floorPlan,
                        heatmapPoints = heatmapPoints,
                        availableSsids = availableSsids,
                        selectedSsid = ssid
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HeatmapUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
