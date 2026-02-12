package id.klaras.wifilogger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.klaras.wifilogger.data.entity.WifiLog
import id.klaras.wifilogger.data.entity.WifiLogWithFloorPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiLogDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wifiLog: WifiLog): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wifiLogs: List<WifiLog>): List<Long>
    
    @Update
    suspend fun update(wifiLog: WifiLog)
    
    @Delete
    suspend fun delete(wifiLog: WifiLog)
    
    @Query("DELETE FROM wifi_logs WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM wifi_logs WHERE floorPlanId = :floorPlanId")
    suspend fun deleteByFloorPlanId(floorPlanId: Long)
    
    @Query("SELECT * FROM wifi_logs WHERE id = :id")
    suspend fun getById(id: Long): WifiLog?
    
    @Query("SELECT * FROM wifi_logs WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<WifiLog?>
    
    @Query("SELECT * FROM wifi_logs WHERE floorPlanId = :floorPlanId ORDER BY timestamp DESC")
    fun getLogsByFloorPlanId(floorPlanId: Long): Flow<List<WifiLog>>
    
    @Query("SELECT * FROM wifi_logs ORDER BY timestamp DESC")
    fun getAllWifiLogs(): Flow<List<WifiLog>>
    
    @Query("SELECT * FROM wifi_logs WHERE ssid LIKE '%' || :query || '%'")
    fun searchBySsid(query: String): Flow<List<WifiLog>>
    
    @Query("SELECT COUNT(*) FROM wifi_logs")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM wifi_logs WHERE floorPlanId = :floorPlanId")
    suspend fun getCountByFloorPlanId(floorPlanId: Long): Int
    
    @Query("""
        SELECT wifi_logs.*, floor_plans.name as floorPlanName, floor_plans.id as floorPlanId 
        FROM wifi_logs 
        INNER JOIN floor_plans ON wifi_logs.floorPlanId = floor_plans.id 
        ORDER BY floor_plans.name, wifi_logs.timestamp DESC
    """)
    fun getAllLogsWithFloorPlan(): Flow<List<WifiLogWithFloorPlan>>
    
    @Query("""
        SELECT wifi_logs.*, floor_plans.name as floorPlanName, floor_plans.id as floorPlanId 
        FROM wifi_logs 
        INNER JOIN floor_plans ON wifi_logs.floorPlanId = floor_plans.id 
        ORDER BY wifi_logs.timestamp DESC
    """)
    suspend fun getAllLogsWithFloorPlanOnce(): List<WifiLogWithFloorPlan>
    
    @Query("""
        SELECT wifi_logs.*, floor_plans.name as floorPlanName, floor_plans.id as floorPlanId 
        FROM wifi_logs 
        INNER JOIN floor_plans ON wifi_logs.floorPlanId = floor_plans.id 
        WHERE wifi_logs.floorPlanId = :floorPlanId
        ORDER BY wifi_logs.timestamp DESC
    """)
    suspend fun getLogsWithFloorPlanByFloorPlanId(floorPlanId: Long): List<WifiLogWithFloorPlan>
    
    @Query("""
        SELECT DISTINCT floorPlanId, coordinateX, coordinateY, MAX(timestamp) as latestTimestamp
        FROM wifi_logs 
        WHERE floorPlanId = :floorPlanId 
        GROUP BY coordinateX, coordinateY
        ORDER BY latestTimestamp DESC
    """)
    suspend fun getDistinctScanPoints(floorPlanId: Long): List<ScanPointSummary>
    
    @Query("""
        SELECT coordinateX, coordinateY, 
               AVG(rssi) as avgRssi, 
               MIN(rssi) as minRssi, 
               MAX(rssi) as maxRssi,
               COUNT(*) as scanCount
        FROM wifi_logs 
        WHERE floorPlanId = :floorPlanId 
        GROUP BY coordinateX, coordinateY
    """)
    suspend fun getHeatmapPoints(floorPlanId: Long): List<HeatmapPoint>
    
    @Query("""
        SELECT DISTINCT ssid FROM wifi_logs 
        WHERE floorPlanId = :floorPlanId 
        ORDER BY ssid
    """)
    suspend fun getDistinctSsidsForFloorPlan(floorPlanId: Long): List<String>
    
    @Query("""
        SELECT coordinateX, coordinateY, 
               AVG(rssi) as avgRssi, 
               MIN(rssi) as minRssi, 
               MAX(rssi) as maxRssi,
               COUNT(*) as scanCount
        FROM wifi_logs 
        WHERE floorPlanId = :floorPlanId AND ssid = :ssid
        GROUP BY coordinateX, coordinateY
    """)
    suspend fun getHeatmapPointsForSsid(floorPlanId: Long, ssid: String): List<HeatmapPoint>
    
    @Query("""
        SELECT coordinateX, coordinateY, 
               AVG(rssi) as avgRssi, 
               MIN(rssi) as minRssi, 
               MAX(rssi) as maxRssi,
               COUNT(*) as scanCount
        FROM wifi_logs 
        WHERE floorPlanId = :floorPlanId 
              AND frequency >= :minFreq 
              AND frequency <= :maxFreq
        GROUP BY coordinateX, coordinateY
    """)
    suspend fun getHeatmapPointsForFrequency(
        floorPlanId: Long, 
        minFreq: Int, 
        maxFreq: Int
    ): List<HeatmapPoint>
    
    @Query("""
        SELECT coordinateX, coordinateY, 
               AVG(rssi) as avgRssi, 
               MIN(rssi) as minRssi, 
               MAX(rssi) as maxRssi,
               COUNT(*) as scanCount
        FROM wifi_logs 
        WHERE floorPlanId = :floorPlanId 
              AND ssid = :ssid
              AND frequency >= :minFreq 
              AND frequency <= :maxFreq
        GROUP BY coordinateX, coordinateY
    """)
    suspend fun getHeatmapPointsForSsidAndFrequency(
        floorPlanId: Long, 
        ssid: String,
        minFreq: Int, 
        maxFreq: Int
    ): List<HeatmapPoint>
}
