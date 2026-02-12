package id.klaras.wifilogger.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.data.entity.RouterPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface FloorPlanDao {
    
    // Floor Plan operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloorPlan(floorPlan: FloorPlan): Long
    
    @Update
    suspend fun updateFloorPlan(floorPlan: FloorPlan)
    
    @Delete
    suspend fun deleteFloorPlan(floorPlan: FloorPlan)
    
    @Query("DELETE FROM floor_plans WHERE id = :id")
    suspend fun deleteFloorPlanById(id: Long)
    
    @Query("SELECT * FROM floor_plans WHERE id = :id")
    suspend fun getFloorPlanById(id: Long): FloorPlan?
    
    @Query("SELECT * FROM floor_plans WHERE id = :id")
    fun getFloorPlanByIdFlow(id: Long): Flow<FloorPlan?>
    
    @Query("SELECT * FROM floor_plans ORDER BY name ASC")
    fun getAllFloorPlans(): Flow<List<FloorPlan>>
    
    @Query("SELECT * FROM floor_plans ORDER BY name ASC")
    suspend fun getAllFloorPlansOnce(): List<FloorPlan>
    
    @Query("SELECT COUNT(*) FROM floor_plans")
    suspend fun getFloorPlanCount(): Int
    
    // Router Point operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouterPoint(routerPoint: RouterPoint): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouterPoints(routerPoints: List<RouterPoint>): List<Long>
    
    @Delete
    suspend fun deleteRouterPoint(routerPoint: RouterPoint)
    
    @Query("DELETE FROM router_points WHERE id = :id")
    suspend fun deleteRouterPointById(id: Long)
    
    @Query("DELETE FROM router_points WHERE floorPlanId = :floorPlanId")
    suspend fun deleteRouterPointsByFloorPlanId(floorPlanId: Long)
    
    @Query("SELECT * FROM router_points WHERE floorPlanId = :floorPlanId")
    fun getRouterPointsByFloorPlanId(floorPlanId: Long): Flow<List<RouterPoint>>
    
    @Query("SELECT * FROM router_points WHERE floorPlanId = :floorPlanId")
    suspend fun getRouterPointsByFloorPlanIdOnce(floorPlanId: Long): List<RouterPoint>
    
    @Query("SELECT COUNT(*) FROM router_points WHERE floorPlanId = :floorPlanId")
    suspend fun getRouterPointCount(floorPlanId: Long): Int
}
