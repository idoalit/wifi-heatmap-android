package id.klaras.wifilogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.data.entity.RouterPoint
import id.klaras.wifilogger.data.repository.FloorPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FloorPlanViewModel(
    private val repository: FloorPlanRepository
) : ViewModel() {
    
    val floorPlans: StateFlow<List<FloorPlan>> = repository.getAllFloorPlans()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _selectedFloorPlan = MutableStateFlow<FloorPlan?>(null)
    val selectedFloorPlan: StateFlow<FloorPlan?> = _selectedFloorPlan.asStateFlow()
    
    private val _routerPoints = MutableStateFlow<List<RouterPoint>>(emptyList())
    val routerPoints: StateFlow<List<RouterPoint>> = _routerPoints.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun selectFloorPlan(floorPlan: FloorPlan?) {
        _selectedFloorPlan.value = floorPlan
        floorPlan?.let { loadRouterPoints(it.id) }
    }
    
    fun loadFloorPlanById(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _selectedFloorPlan.value = repository.getFloorPlanByIdOnce(id)
                loadRouterPoints(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadRouterPoints(floorPlanId: Long) {
        viewModelScope.launch {
            repository.getRouterPointsByFloorPlanId(floorPlanId).collect { points ->
                _routerPoints.value = points
            }
        }
    }
    
    fun insertFloorPlan(
        name: String,
        imagePath: String,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val floorPlan = FloorPlan(
                    name = name,
                    imagePath = imagePath
                )
                val id = repository.insertFloorPlan(floorPlan)
                onSuccess(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateFloorPlan(floorPlan: FloorPlan) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateFloorPlan(floorPlan)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteFloorPlan(floorPlan: FloorPlan) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteFloorPlan(floorPlan)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteFloorPlanById(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteFloorPlanById(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Router Point operations
    fun addRouterPoint(floorPlanId: Long, coordinateX: Float, coordinateY: Float) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val routerPoint = RouterPoint(
                    floorPlanId = floorPlanId,
                    coordinateX = coordinateX,
                    coordinateY = coordinateY
                )
                repository.insertRouterPoint(routerPoint)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteRouterPoint(routerPoint: RouterPoint) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteRouterPoint(routerPoint)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteRouterPointById(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteRouterPointById(id)
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
