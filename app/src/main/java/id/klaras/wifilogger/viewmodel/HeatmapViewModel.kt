package id.klaras.wifilogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.klaras.wifilogger.data.dao.HeatmapPoint
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.data.repository.FloorPlanRepository
import id.klaras.wifilogger.data.repository.FrequencyBand
import id.klaras.wifilogger.data.repository.WifiLogRepository
import id.klaras.wifilogger.ui.screen.IdwInterpolator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HeatmapUiState {
    data object Loading : HeatmapUiState()
    data class Success(
        val floorPlan: FloorPlan,
        val heatmapPoints: List<HeatmapPoint>,
        val interpolatedGrid: Array<Array<Float>>, // IDW interpolated grid
        val availableSsids: List<String>,
        val selectedSsid: String?, // null means "All Networks"
        val selectedFrequency: FrequencyBand
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

    private val idwInterpolator = IdwInterpolator(power = 2.0)
    private var currentFloorPlanId: Long = -1
    private var currentFloorPlan: FloorPlan? = null
    private var availableSsids: List<String> = emptyList()
    private var currentFrequency: FrequencyBand = FrequencyBand.ALL

    fun loadHeatmap(floorPlanIdString: String) {
        val floorPlanId = floorPlanIdString.toLongOrNull()
        if (floorPlanId == null) {
            _uiState.value = HeatmapUiState.Error("Invalid floor plan ID")
            return
        }

        currentFloorPlanId = floorPlanId
        currentFrequency = FrequencyBand.ALL
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
                val heatmapPoints = wifiLogRepository.getHeatmapPointsForSsidAndFrequency(
                    floorPlanId, 
                    null, 
                    FrequencyBand.ALL
                )

                if (heatmapPoints.isEmpty()) {
                    _uiState.value = HeatmapUiState.NoData
                } else {
                    // Generate interpolated grid using IDW
                    val interpolatedGrid = idwInterpolator.interpolate(heatmapPoints)
                    
                    _uiState.value = HeatmapUiState.Success(
                        floorPlan = floorPlan,
                        heatmapPoints = heatmapPoints,
                        interpolatedGrid = interpolatedGrid,
                        availableSsids = availableSsids,
                        selectedSsid = null,
                        selectedFrequency = FrequencyBand.ALL
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
                
                val heatmapPoints = wifiLogRepository.getHeatmapPointsForSsidAndFrequency(
                    currentFloorPlanId, 
                    ssid, 
                    currentFrequency
                )

                if (heatmapPoints.isEmpty()) {
                    _uiState.value = HeatmapUiState.NoData
                } else {
                    // Generate interpolated grid using IDW
                    val interpolatedGrid = idwInterpolator.interpolate(heatmapPoints)
                    
                    _uiState.value = HeatmapUiState.Success(
                        floorPlan = floorPlan,
                        heatmapPoints = heatmapPoints,
                        interpolatedGrid = interpolatedGrid,
                        availableSsids = availableSsids,
                        selectedSsid = ssid,
                        selectedFrequency = currentFrequency
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HeatmapUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun selectFrequency(frequencyBand: FrequencyBand) {
        val floorPlan = currentFloorPlan ?: return
        val currentState = _uiState.value as? HeatmapUiState.Success ?: return
        
        currentFrequency = frequencyBand
        
        viewModelScope.launch {
            try {
                _uiState.value = HeatmapUiState.Loading
                
                val heatmapPoints = wifiLogRepository.getHeatmapPointsForSsidAndFrequency(
                    currentFloorPlanId,
                    currentState.selectedSsid,
                    frequencyBand
                )

                if (heatmapPoints.isEmpty()) {
                    _uiState.value = HeatmapUiState.NoData
                } else {
                    // Generate interpolated grid using IDW
                    val interpolatedGrid = idwInterpolator.interpolate(heatmapPoints)
                    
                    _uiState.value = HeatmapUiState.Success(
                        floorPlan = floorPlan,
                        heatmapPoints = heatmapPoints,
                        interpolatedGrid = interpolatedGrid,
                        availableSsids = availableSsids,
                        selectedSsid = currentState.selectedSsid,
                        selectedFrequency = frequencyBand
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HeatmapUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
