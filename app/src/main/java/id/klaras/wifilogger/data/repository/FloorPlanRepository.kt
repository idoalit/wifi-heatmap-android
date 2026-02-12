package id.klaras.wifilogger.data.repository

import id.klaras.wifilogger.data.dao.FloorPlanDao
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.data.entity.RouterPoint
import kotlinx.coroutines.flow.Flow

class FloorPlanRepository(
    private val floorPlanDao: FloorPlanDao
) {
    
    // Floor Plan operations
    fun getAllFloorPlans(): Flow<List<FloorPlan>> = floorPlanDao.getAllFloorPlans()
    
    fun getFloorPlanById(id: Long): Flow<FloorPlan?> = floorPlanDao.getFloorPlanByIdFlow(id)
    
    suspend fun getFloorPlanByIdOnce(id: Long): FloorPlan? = floorPlanDao.getFloorPlanById(id)
    
    suspend fun getAllFloorPlansOnce(): List<FloorPlan> = floorPlanDao.getAllFloorPlansOnce()
    
    suspend fun insertFloorPlan(floorPlan: FloorPlan): Long = floorPlanDao.insertFloorPlan(floorPlan)
    
    suspend fun updateFloorPlan(floorPlan: FloorPlan) = floorPlanDao.updateFloorPlan(floorPlan)
    
    suspend fun deleteFloorPlan(floorPlan: FloorPlan) = floorPlanDao.deleteFloorPlan(floorPlan)
    
    suspend fun deleteFloorPlanById(id: Long) = floorPlanDao.deleteFloorPlanById(id)
    
    suspend fun getFloorPlanCount(): Int = floorPlanDao.getFloorPlanCount()
    
    // Router Point operations
    fun getRouterPointsByFloorPlanId(floorPlanId: Long): Flow<List<RouterPoint>> = 
        floorPlanDao.getRouterPointsByFloorPlanId(floorPlanId)
    
    suspend fun getRouterPointsByFloorPlanIdOnce(floorPlanId: Long): List<RouterPoint> = 
        floorPlanDao.getRouterPointsByFloorPlanIdOnce(floorPlanId)
    
    suspend fun insertRouterPoint(routerPoint: RouterPoint): Long = 
        floorPlanDao.insertRouterPoint(routerPoint)
    
    suspend fun insertRouterPoints(routerPoints: List<RouterPoint>): List<Long> = 
        floorPlanDao.insertRouterPoints(routerPoints)
    
    suspend fun deleteRouterPoint(routerPoint: RouterPoint) = 
        floorPlanDao.deleteRouterPoint(routerPoint)
    
    suspend fun deleteRouterPointById(id: Long) = 
        floorPlanDao.deleteRouterPointById(id)
    
    suspend fun deleteRouterPointsByFloorPlanId(floorPlanId: Long) = 
        floorPlanDao.deleteRouterPointsByFloorPlanId(floorPlanId)
    
    suspend fun getRouterPointCount(floorPlanId: Long): Int = 
        floorPlanDao.getRouterPointCount(floorPlanId)
}
