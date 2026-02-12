package id.klaras.wifilogger.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "router_points",
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
data class RouterPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val floorPlanId: Long,
    val coordinateX: Float,
    val coordinateY: Float
)
