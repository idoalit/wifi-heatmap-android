package id.klaras.wifilogger.ui.screen

import id.klaras.wifilogger.data.dao.HeatmapPoint
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * IDW (Inverse Distance Weighting) interpolation engine for heatmap generation.
 * Generates a 2D grid of interpolated signal strength values from discrete scan points.
 */
class IdwInterpolator(
    private val power: Double = 2.0
) {
    
    /**
     * Interpolates a 2D grid of RSSI values from discrete scan points using IDW.
     * 
     * @param points List of heatmap points with coordinates (0-100%) and RSSI values
     * @param gridWidth Number of grid cells in width (default 100 for percentage-based)
     * @param gridHeight Number of grid cells in height (default 100 for percentage-based)
     * @return 2D array where [x][y] contains the interpolated RSSI value (-30 to -100 dBm)
     */
    fun interpolate(
        points: List<HeatmapPoint>,
        gridWidth: Int = 100,
        gridHeight: Int = 100
    ): Array<Array<Float>> {
        if (points.isEmpty()) {
            return Array(gridWidth) { Array(gridHeight) { -100f } }
        }
        
        val grid = Array(gridWidth) { Array(gridHeight) { 0f } }
        
        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                // Grid coordinates as percentages (0-100)
                val gridX = (x.toFloat() / (gridWidth - 1)) * 100f
                val gridY = (y.toFloat() / (gridHeight - 1)) * 100f
                
                grid[x][y] = calculateIdw(gridX, gridY, points)
            }
        }
        
        return grid
    }
    
    /**
     * Calculates the IDW-interpolated RSSI value at a given point.
     * 
     * Formula: Σ(wi * vi) / Σ(wi), where wi = 1 / distance^power
     * 
     * @param x X coordinate (0-100%)
     * @param y Y coordinate (0-100%)
     * @param points List of scan points with known RSSI values
     * @return Interpolated RSSI value in dBm
     */
    private fun calculateIdw(
        x: Float,
        y: Float,
        points: List<HeatmapPoint>
    ): Float {
        var weightedSum = 0.0
        var weightSum = 0.0
        
        // Threshold for considering a point "at the same location"
        val minDistance = 0.1f
        
        for (point in points) {
            val dx = x - point.coordinateX
            val dy = y - point.coordinateY
            val distance = sqrt(dx * dx + dy * dy)
            
            // If we're very close to an actual scan point, use its value directly
            if (distance < minDistance) {
                return point.avgRssi.toFloat()
            }
            
            // Calculate weight: 1 / distance^power
            val weight = 1.0 / distance.toDouble().pow(power)
            
            weightedSum += weight * point.avgRssi
            weightSum += weight
        }
        
        return if (weightSum > 0) {
            (weightedSum / weightSum).toFloat()
        } else {
            -100f // Default weak signal if calculation fails
        }
    }
}
