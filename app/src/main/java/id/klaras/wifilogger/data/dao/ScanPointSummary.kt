package id.klaras.wifilogger.data.dao

/**
 * Summary data for distinct scan points on a floor plan
 */
data class ScanPointSummary(
    val floorPlanId: Long,
    val coordinateX: Float,
    val coordinateY: Float,
    val latestTimestamp: Long
)
