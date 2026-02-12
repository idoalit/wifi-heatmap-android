package id.klaras.wifilogger.data.dao

/**
 * Data class representing a point on the heatmap with aggregated RSSI data
 */
data class HeatmapPoint(
    val coordinateX: Float,
    val coordinateY: Float,
    val avgRssi: Double,
    val minRssi: Int,
    val maxRssi: Int,
    val scanCount: Int
)
