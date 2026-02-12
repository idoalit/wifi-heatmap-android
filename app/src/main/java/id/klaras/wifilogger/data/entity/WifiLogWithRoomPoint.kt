package id.klaras.wifilogger.data.entity

import androidx.room.Embedded

/**
 * Data class that combines WifiLog with RoomPoint information for display and export
 */
data class WifiLogWithRoomPoint(
    @Embedded val wifiLog: WifiLog,
    val roomPointName: String
)
