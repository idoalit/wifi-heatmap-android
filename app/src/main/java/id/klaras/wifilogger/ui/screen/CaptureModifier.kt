package id.klaras.wifilogger.ui.screen

import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap

/**
 * Modifier to make a Composable capturable as a bitmap
 * 
 * Usage:
 * ```
 * val controller = rememberCaptureController()
 * 
 * Box(modifier = Modifier.capturable(controller)) {
 *     // Your content here
 * }
 * 
 * // To capture:
 * controller.capture { bitmap ->
 *     // Use bitmap
 * }
 * ```
 */
fun Modifier.capturable(controller: CaptureController): Modifier = composed {
    val view = LocalView.current
    
    LaunchedEffect(controller.captureRequested) {
        if (controller.captureRequested) {
            try {
                // Небольшая задержка для завершения рендеринга
                kotlinx.coroutines.delay(100)
                
                val bitmap = captureView(view)
                if (bitmap != null) {
                    controller.captured(bitmap.asImageBitmap())
                } else {
                    controller.reset()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                controller.reset()
            }
        }
    }
    
    this
}

/**
 * Captures a View as a Bitmap
 */
private fun captureView(view: View): Bitmap? {
    return try {
        // Find root view for full capture
        val rootView = view.rootView
        rootView.drawToBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
