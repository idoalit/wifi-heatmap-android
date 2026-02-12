package id.klaras.wifilogger.data.entity

import androidx.room.Embedded

/**
 * Data class that combines WifiLog with FloorPlan information for display and export
 */
data class WifiLogWithFloorPlan(
    @Embedded val wifiLog: WifiLog,
    val floorPlanName: String
)
