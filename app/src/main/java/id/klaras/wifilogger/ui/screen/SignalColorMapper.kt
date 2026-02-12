package id.klaras.wifilogger.ui.screen

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Maps WiFi signal strength (RSSI in dBm) to colors with alpha transparency.
 * 
 * Color scale (with smooth interpolation):
 * - -30 to -50 dBm → Green (excellent signal)
 * - -50 to -60 dBm → Yellow (good signal)
 * - -60 to -70 dBm → Orange (fair signal)
 * - -70 to -80 dBm → Light Red (poor signal)
 * - -80 to -90 dBm → Dark Red (very weak signal)
 * - Below -90 dBm → Transparent (no usable signal)
 */
object SignalColorMapper {
    
    // Color definitions
    private val excellentGreen = Color(0xFF00C853) // Material Green A700
    private val goodYellow = Color(0xFFFDD835)     // Material Yellow 600
    private val fairOrange = Color(0xFFFF9800)     // Material Orange 500
    private val poorRed = Color(0xFFFF5252)        // Material Red A200
    private val veryWeakRed = Color(0xFFD32F2F)    // Material Red 700
    
    // RSSI thresholds (in dBm)
    private val thresholds = listOf(-30f, -50f, -60f, -70f, -80f, -90f)
    
    // Alpha values for each range (decreasing with weaker signal)
    private val alphaValues = listOf(0.75f, 0.7f, 0.65f, 0.6f, 0.5f, 0.35f)
    
    /**
     * Converts RSSI value to a Color with appropriate alpha transparency.
     * 
     * @param rssi Signal strength in dBm (typically -30 to -100)
     * @return Compose Color with alpha applied
     */
    fun rssiToColor(rssi: Float): Color {
        // Handle extreme values
        if (rssi >= -30f) {
            return excellentGreen.copy(alpha = alphaValues[0])
        }
        if (rssi <= -90f) {
            return veryWeakRed.copy(alpha = alphaValues[5])
        }
        
        // Determine which range the RSSI falls into and interpolate
        return when {
            rssi > -50f -> {
                // -30 to -50: Green
                val t = (-30f - rssi) / 20f // 0 to 1
                val alpha = lerp(alphaValues[0], alphaValues[1], t)
                excellentGreen.copy(alpha = alpha)
            }
            rssi > -60f -> {
                // -50 to -60: Green to Yellow
                val t = (-50f - rssi) / 10f // 0 to 1
                val alpha = lerp(alphaValues[1], alphaValues[2], t)
                lerpColor(excellentGreen, goodYellow, t).copy(alpha = alpha)
            }
            rssi > -70f -> {
                // -60 to -70: Yellow to Orange
                val t = (-60f - rssi) / 10f // 0 to 1
                val alpha = lerp(alphaValues[2], alphaValues[3], t)
                lerpColor(goodYellow, fairOrange, t).copy(alpha = alpha)
            }
            rssi > -80f -> {
                // -70 to -80: Orange to Light Red
                val t = (-70f - rssi) / 10f // 0 to 1
                val alpha = lerp(alphaValues[3], alphaValues[4], t)
                lerpColor(fairOrange, poorRed, t).copy(alpha = alpha)
            }
            else -> {
                // -80 to -90: Light Red to Dark Red
                val t = (-80f - rssi) / 10f // 0 to 1
                val alpha = lerp(alphaValues[4], alphaValues[5], t)
                lerpColor(poorRed, veryWeakRed, t).copy(alpha = alpha)
            }
        }
    }
    
    /**
     * Linear interpolation between two float values.
     */
    private fun lerp(start: Float, end: Float, t: Float): Float {
        val clamped = max(0f, min(1f, t))
        return start + (end - start) * clamped
    }
    
    /**
     * Linear interpolation between two colors (RGB components).
     */
    private fun lerpColor(start: Color, end: Color, t: Float): Color {
        val clamped = max(0f, min(1f, t))
        return Color(
            red = lerp(start.red, end.red, clamped),
            green = lerp(start.green, end.green, clamped),
            blue = lerp(start.blue, end.blue, clamped),
            alpha = 1f // Alpha will be applied separately
        )
    }
    
    /**
     * Returns a descriptive label for the signal strength.
     */
    fun rssiToLabel(rssi: Float): String {
        return when {
            rssi >= -50f -> "Excellent"
            rssi >= -60f -> "Good"
            rssi >= -70f -> "Fair"
            rssi >= -80f -> "Poor"
            rssi >= -90f -> "Very Weak"
            else -> "No Signal"
        }
    }
}
