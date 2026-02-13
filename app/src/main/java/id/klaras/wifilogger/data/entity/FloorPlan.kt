package id.klaras.wifilogger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "floor_plans")
data class FloorPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imagePath: String,
    val remoteId: String? = null
)
