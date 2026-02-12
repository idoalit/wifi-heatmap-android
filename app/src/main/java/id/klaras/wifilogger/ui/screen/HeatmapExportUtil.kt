package id.klaras.wifilogger.ui.screen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for capturing Composable content as bitmap and exporting it
 */
object HeatmapExportUtil {
    
    /**
     * Saves a bitmap to device gallery using MediaStore
     * 
     * @param context Android context
     * @param bitmap The bitmap to save
     * @param fileName Optional custom filename
     * @return URI of saved image or null if failed
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = generateFileName()
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WiFiHeatmap")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            
            val contentResolver = context.contentResolver
            val imageUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            imageUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                
                return@withContext uri
            }
            
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    /**
     * Creates a shareable URI for a bitmap and opens share intent
     * 
     * @param context Android context
     * @param bitmap The bitmap to share
     * @return URI for sharing or null if failed
     */
    suspend fun shareBitmap(
        context: Context,
        bitmap: Bitmap
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // Save to cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            
            val file = File(cachePath, generateFileName())
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            // Get URI using FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            return@withContext uri
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    /**
     * Opens Android share sheet with the bitmap
     * 
     * @param context Android context
     * @param uri URI of the image to share
     */
    fun openShareSheet(context: Context, uri: Uri) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(
            Intent.createChooser(shareIntent, "Share WiFi Heatmap")
        )
    }
    
    /**
     * Generates a timestamped filename for the heatmap export
     */
    private fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "WiFiHeatmap_$timestamp.png"
    }
}

/**
 * Helper class to track the state of a capturable Composable
 */
class CaptureController {
    var captureRequested: Boolean = false
        private set
    
    var onCaptured: ((ImageBitmap) -> Unit)? = null
        private set
    
    fun capture(onResult: (ImageBitmap) -> Unit) {
        onCaptured = onResult
        captureRequested = true
    }
    
    fun captured(bitmap: ImageBitmap) {
        onCaptured?.invoke(bitmap)
        captureRequested = false
        onCaptured = null
    }
    
    fun reset() {
        captureRequested = false
        onCaptured = null
    }
}

/**
 * Remember a CaptureController for capturing Composable content
 */
@Composable
fun rememberCaptureController(): CaptureController {
    return remember { CaptureController() }
}
