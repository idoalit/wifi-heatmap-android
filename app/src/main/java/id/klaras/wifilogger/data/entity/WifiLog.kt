package id.klaras.wifilogger.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wifi_logs",
    foreignKeys = [
        ForeignKey(
            entity = FloorPlan::class,
            parentColumns = ["id"],
            childColumns = ["floorPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["floorPlanId"])]
)
data class WifiLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val floorPlanId: Long,
    val coordinateX: Float,
    val coordinateY: Float,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val timestamp: Long
)
