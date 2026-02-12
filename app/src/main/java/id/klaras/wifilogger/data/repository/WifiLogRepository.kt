package id.klaras.wifilogger.data.repository

import id.klaras.wifilogger.data.dao.HeatmapPoint
import id.klaras.wifilogger.data.dao.ScanPointSummary
import id.klaras.wifilogger.data.dao.WifiLogDao
import id.klaras.wifilogger.data.entity.WifiLog
import id.klaras.wifilogger.data.entity.WifiLogWithFloorPlan
import kotlinx.coroutines.flow.Flow

class WifiLogRepository(
    private val wifiLogDao: WifiLogDao
) {
    
    fun getAllWifiLogs(): Flow<List<WifiLog>> = wifiLogDao.getAllWifiLogs()
    
    fun getWifiLogById(id: Long): Flow<WifiLog?> = wifiLogDao.getByIdFlow(id)
    
    fun getLogsByFloorPlanId(floorPlanId: Long): Flow<List<WifiLog>> = 
        wifiLogDao.getLogsByFloorPlanId(floorPlanId)
    
    fun searchBySsid(query: String): Flow<List<WifiLog>> = wifiLogDao.searchBySsid(query)
    
    fun getAllLogsWithFloorPlan(): Flow<List<WifiLogWithFloorPlan>> = 
        wifiLogDao.getAllLogsWithFloorPlan()
    
    suspend fun getWifiLogByIdOnce(id: Long): WifiLog? = wifiLogDao.getById(id)
    
    suspend fun getAllLogsWithFloorPlanForExport(): List<WifiLogWithFloorPlan> = 
        wifiLogDao.getAllLogsWithFloorPlanOnce()
    
    suspend fun getLogsWithFloorPlanByFloorPlanId(floorPlanId: Long): List<WifiLogWithFloorPlan> = 
        wifiLogDao.getLogsWithFloorPlanByFloorPlanId(floorPlanId)
    
    suspend fun getDistinctScanPoints(floorPlanId: Long): List<ScanPointSummary> =
        wifiLogDao.getDistinctScanPoints(floorPlanId)
    
    suspend fun insert(wifiLog: WifiLog): Long = wifiLogDao.insert(wifiLog)
    
    suspend fun insertAll(wifiLogs: List<WifiLog>): List<Long> = wifiLogDao.insertAll(wifiLogs)
    
    suspend fun update(wifiLog: WifiLog) = wifiLogDao.update(wifiLog)
    
    suspend fun delete(wifiLog: WifiLog) = wifiLogDao.delete(wifiLog)
    
    suspend fun deleteById(id: Long) = wifiLogDao.deleteById(id)
    
    suspend fun deleteByFloorPlanId(floorPlanId: Long) = wifiLogDao.deleteByFloorPlanId(floorPlanId)
    
    suspend fun getCount(): Int = wifiLogDao.getCount()
    
    suspend fun getCountByFloorPlanId(floorPlanId: Long): Int = 
        wifiLogDao.getCountByFloorPlanId(floorPlanId)
    
    // Heatmap operations
    suspend fun getHeatmapPoints(floorPlanId: Long) = wifiLogDao.getHeatmapPoints(floorPlanId)
    
    suspend fun getDistinctSsidsForFloorPlan(floorPlanId: Long) = 
        wifiLogDao.getDistinctSsidsForFloorPlan(floorPlanId)
    
    suspend fun getHeatmapPointsForSsid(floorPlanId: Long, ssid: String) = 
        wifiLogDao.getHeatmapPointsForSsid(floorPlanId, ssid)
}
