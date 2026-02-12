package id.klaras.wifilogger.data

import androidx.room.Database
import androidx.room.RoomDatabase
import id.klaras.wifilogger.data.dao.FloorPlanDao
import id.klaras.wifilogger.data.dao.WifiLogDao
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.data.entity.RouterPoint
import id.klaras.wifilogger.data.entity.WifiLog

@Database(
    entities = [FloorPlan::class, RouterPoint::class, WifiLog::class],
    version = 3,
    exportSchema = false
)
abstract class WifiLoggerDatabase : RoomDatabase() {
    
    abstract fun floorPlanDao(): FloorPlanDao
    abstract fun wifiLogDao(): WifiLogDao
    
    companion object {
        const val DATABASE_NAME = "wifi_logger_database"
    }
}
