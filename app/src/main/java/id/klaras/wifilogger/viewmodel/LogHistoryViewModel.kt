package id.klaras.wifilogger.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.klaras.wifilogger.data.entity.RouterPoint
import id.klaras.wifilogger.data.entity.WifiLogWithFloorPlan
import id.klaras.wifilogger.data.repository.FloorPlanRepository
import id.klaras.wifilogger.data.repository.WifiLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogHistoryViewModel(
    private val wifiLogRepository: WifiLogRepository,
    private val floorPlanRepository: FloorPlanRepository
) : ViewModel() {
    
    private val _logsWithFloorPlan = MutableStateFlow<List<WifiLogWithFloorPlan>>(emptyList())
    val logsWithFloorPlan: StateFlow<List<WifiLogWithFloorPlan>> = _logsWithFloorPlan.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()
    
    init {
        loadLogs()
    }
    
    private fun loadLogs() {
        viewModelScope.launch {
            wifiLogRepository.getAllLogsWithFloorPlan().collect { logs ->
                _logsWithFloorPlan.value = logs
            }
        }
    }
    
    fun getLogsGroupedByFloorPlan(): Map<String, List<WifiLogWithFloorPlan>> {
        return _logsWithFloorPlan.value.groupBy { it.floorPlanName }
    }
    
    fun getLogsGroupedByFloorPlanWithId(): Map<Pair<Long, String>, List<WifiLogWithFloorPlan>> {
        return _logsWithFloorPlan.value.groupBy { Pair(it.wifiLog.floorPlanId, it.floorPlanName) }
    }
    
    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val logs = wifiLogRepository.getAllLogsWithFloorPlanForExport()
                if (logs.isEmpty()) {
                    _exportResult.value = ExportResult.Error("Tidak ada data untuk diekspor")
                    return@launch
                }
                
                // Get all unique floor plan IDs and fetch their router points
                val floorPlanIds = logs.map { it.wifiLog.floorPlanId }.distinct()
                val routerPointsMap = mutableMapOf<Long, List<RouterPoint>>()
                floorPlanIds.forEach { floorPlanId ->
                    routerPointsMap[floorPlanId] = floorPlanRepository.getRouterPointsByFloorPlanIdOnce(floorPlanId)
                }
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                val fileName = "WifiLog_${dateFormat.format(Date())}.csv"
                
                val csvContent = buildCsvContent(logs, routerPointsMap)
                
                val filePath = saveToDownloads(context, fileName, csvContent)
                _exportResult.value = ExportResult.Success(filePath)
                
            } catch (e: Exception) {
                _exportResult.value = ExportResult.Error("Gagal mengekspor: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun exportFloorPlanToCsv(context: Context, floorPlanId: Long, floorPlanName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val logs = wifiLogRepository.getLogsWithFloorPlanByFloorPlanId(floorPlanId)
                if (logs.isEmpty()) {
                    _exportResult.value = ExportResult.Error("Tidak ada data untuk diekspor")
                    return@launch
                }
                
                val routerPoints = floorPlanRepository.getRouterPointsByFloorPlanIdOnce(floorPlanId)
                val routerPointsMap = mapOf(floorPlanId to routerPoints)
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                val safeFloorPlanName = floorPlanName.replace(Regex("[^a-zA-Z0-9]"), "_")
                val fileName = "WifiLog_${safeFloorPlanName}_${dateFormat.format(Date())}.csv"
                
                val csvContent = buildCsvContent(logs, routerPointsMap)
                
                val filePath = saveToDownloads(context, fileName, csvContent)
                _exportResult.value = ExportResult.Success(filePath)
                
            } catch (e: Exception) {
                _exportResult.value = ExportResult.Error("Gagal mengekspor: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun buildCsvContent(
        logs: List<WifiLogWithFloorPlan>,
        routerPointsMap: Map<Long, List<RouterPoint>>
    ): String {
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        return buildString {
            // Header
            appendLine("Timestamp,Floor Plan,Log X,Log Y,Router Coordinates,SSID,BSSID,RSSI,Frequency")
            
            // Data rows
            logs.forEach { log ->
                val timestamp = timestampFormat.format(Date(log.wifiLog.timestamp))
                val floorPlanName = escapeCsvField(log.floorPlanName)
                val x = log.wifiLog.coordinateX
                val y = log.wifiLog.coordinateY
                
                // Format router coordinates
                val routerPoints = routerPointsMap[log.wifiLog.floorPlanId] ?: emptyList()
                val routerCoords = if (routerPoints.isNotEmpty()) {
                    routerPoints.joinToString("; ") { "(${it.coordinateX}, ${it.coordinateY})" }
                } else {
                    "No router"
                }
                val routerCoordsEscaped = escapeCsvField(routerCoords)
                
                val ssid = escapeCsvField(log.wifiLog.ssid)
                val bssid = escapeCsvField(log.wifiLog.bssid)
                val rssi = log.wifiLog.rssi
                val frequency = log.wifiLog.frequency
                
                appendLine("$timestamp,$floorPlanName,$x,$y,$routerCoordsEscaped,$ssid,$bssid,$rssi,$frequency")
            }
        }
    }
    
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
    
    private fun saveToDownloads(context: Context, fileName: String, content: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Gagal membuat file")
            
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            
            "Downloads/$fileName"
        } else {
            // For Android 9 and below
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            file.absolutePath
        }
    }
    
    fun deleteFloorPlanLogs(floorPlanId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                wifiLogRepository.deleteByFloorPlanId(floorPlanId)
                _exportResult.value = ExportResult.Success("Logs berhasil dihapus")
            } catch (e: Exception) {
                _exportResult.value = ExportResult.Error("Gagal menghapus logs: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }
    
    sealed class ExportResult {
        data class Success(val filePath: String) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }
}
