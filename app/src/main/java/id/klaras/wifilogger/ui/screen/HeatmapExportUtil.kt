package id.klaras.wifilogger.ui.screen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import id.klaras.wifilogger.data.dao.HeatmapPoint
import id.klaras.wifilogger.data.entity.FloorPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Utility for rendering and exporting WiFi heatmap bitmaps.
 */
object HeatmapExportUtil {

    suspend fun renderHeatmapBitmap(
        floorPlan: FloorPlan,
        interpolatedGrid: Array<Array<Float>>,
        heatmapPoints: List<HeatmapPoint>,
        selectedSsid: String?
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val sourceBitmap = BitmapFactory.decodeFile(
                floorPlan.imagePath,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            ) ?: return@withContext null

            val exportBitmap = if (sourceBitmap.isMutable) {
                sourceBitmap
            } else {
                sourceBitmap.copy(Bitmap.Config.ARGB_8888, true).also {
                    sourceBitmap.recycle()
                }
            }

            val canvas = Canvas(exportBitmap)
            drawHeatmapOverlay(
                canvas = canvas,
                width = exportBitmap.width,
                height = exportBitmap.height,
                interpolatedGrid = interpolatedGrid
            )
            drawTitle(
                canvas = canvas,
                width = exportBitmap.width,
                height = exportBitmap.height,
                floorPlanName = floorPlan.name,
                selectedSsid = selectedSsid
            )
            drawScanMarkers(
                canvas = canvas,
                width = exportBitmap.width,
                height = exportBitmap.height,
                heatmapPoints = heatmapPoints
            )
            drawLegend(
                canvas = canvas,
                width = exportBitmap.width,
                height = exportBitmap.height
            )

            return@withContext exportBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
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

    private fun drawHeatmapOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        interpolatedGrid: Array<Array<Float>>
    ) {
        if (interpolatedGrid.isEmpty() || interpolatedGrid[0].isEmpty()) {
            return
        }

        val gridWidth = interpolatedGrid.size
        val gridHeight = interpolatedGrid[0].size
        val cellWidth = width.toFloat() / gridWidth
        val cellHeight = height.toFloat() / gridHeight

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (x in 0 until gridWidth) {
            for (y in 0 until gridHeight) {
                paint.color = SignalColorMapper.rssiToColor(interpolatedGrid[x][y]).toArgb()

                val left = x * cellWidth
                val top = y * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight
                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
    }

    private fun drawScanMarkers(
        canvas: Canvas,
        width: Int,
        height: Int,
        heatmapPoints: List<HeatmapPoint>
    ) {
        if (heatmapPoints.isEmpty()) {
            return
        }

        val minDimension = min(width, height).toFloat()
        val outerRadius = min(12f, max(4f, minDimension * 0.0055f))
        val innerRadius = outerRadius * 0.67f

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#424242")
            style = Paint.Style.FILL
        }

        heatmapPoints.forEach { point ->
            val centerX = (point.coordinateX / 100f) * width
            val centerY = (point.coordinateY / 100f) * height

            canvas.drawCircle(centerX, centerY, outerRadius, outlinePaint)
            canvas.drawCircle(centerX, centerY, innerRadius, centerPaint)
        }
    }

    private fun drawTitle(
        canvas: Canvas,
        width: Int,
        height: Int,
        floorPlanName: String,
        selectedSsid: String?
    ) {
        val minDimension = min(width, height).toFloat()
        val padding = max(10f, minDimension * 0.02f)
        val titleText = "WiFi Heatmap - ${floorPlanName.ifBlank { "Floor Plan" }}"
        val ssidText = "SSID: ${selectedSsid ?: "All Networks"}"

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(12f, minDimension * 0.022f)
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(9f, minDimension * 0.016f)
        }

        val horizontalInset = max(14f, minDimension * 0.025f)
        val maxTextWidth = width - (2f * (padding + horizontalInset))
        while (titlePaint.measureText(titleText) > maxTextWidth && titlePaint.textSize > 8f) {
            titlePaint.textSize -= 0.5f
        }
        while (subtitlePaint.measureText(ssidText) > maxTextWidth && subtitlePaint.textSize > 7f) {
            subtitlePaint.textSize -= 0.5f
        }

        val titleMetrics = titlePaint.fontMetrics
        val subtitleMetrics = subtitlePaint.fontMetrics
        val titleHeight = titleMetrics.descent - titleMetrics.ascent
        val subtitleHeight = subtitleMetrics.descent - subtitleMetrics.ascent
        val lineGap = max(4f, minDimension * 0.006f)
        val cardHeight = titleHeight + subtitleHeight + lineGap + (2f * padding)
        val textBlockWidth = max(titlePaint.measureText(titleText), subtitlePaint.measureText(ssidText))
        val cardWidth = min(width * 0.82f, textBlockWidth + (2f * horizontalInset))
        val left = padding
        val top = padding
        val right = left + cardWidth
        val bottom = top + cardHeight

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(175, 20, 20, 20)
            style = Paint.Style.FILL
        }
        val cornerRadius = max(8f, minDimension * 0.015f)
        canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, cardPaint)

        val textX = left + horizontalInset
        val titleY = top + padding - titleMetrics.ascent
        val subtitleY = titleY + titleMetrics.descent - subtitleMetrics.ascent + lineGap
        canvas.drawText(titleText, textX, titleY, titlePaint)
        canvas.drawText(ssidText, textX, subtitleY, subtitlePaint)
    }

    private fun drawLegend(
        canvas: Canvas,
        width: Int,
        height: Int
    ) {
        val minDimension = min(width, height).toFloat()
        val padding = max(10f, minDimension * 0.02f)
        val cardHeight = max(54f, minDimension * 0.11f)
        val cardWidth = min(width * 0.5f, 340f)
        val left = padding
        val top = height - cardHeight - padding
        val right = left + cardWidth
        val bottom = top + cardHeight

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 20, 20, 20)
            style = Paint.Style.FILL
        }
        val cornerRadius = max(8f, minDimension * 0.015f)
        val cardRect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(12f, minDimension * 0.02f)
            isFakeBoldText = true
        }

        val labelBaseSize = max(9f, minDimension * 0.015f)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = labelBaseSize
        }

        val titleBaseline = top + padding + titlePaint.textSize
        canvas.drawText("Signal Strength", left + padding, titleBaseline, titlePaint)

        val barTop = titleBaseline + max(5f, minDimension * 0.007f)
        val barBottom = barTop + max(9f, minDimension * 0.02f)
        val barLeft = left + padding
        val barRight = right - padding
        val barRect = RectF(barLeft, barTop, barRight, barBottom)

        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                barLeft,
                barTop,
                barRight,
                barTop,
                intArrayOf(
                    Color.parseColor("#00C853"),
                    Color.parseColor("#FFEB3B"),
                    Color.parseColor("#FF9800"),
                    Color.parseColor("#F44336")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(barRect, cornerRadius * 0.4f, cornerRadius * 0.4f, gradientPaint)

        val fullStrongLabel = "Strong (-30 dBm)"
        val fullWeakLabel = "Weak (-100 dBm)"
        val shortStrongLabel = "Strong (-30)"
        val shortWeakLabel = "Weak (-100)"
        val availableWidth = barRight - barLeft
        val minGap = max(10f, minDimension * 0.012f)
        val minLabelTextSize = 7f

        var strongLabel = fullStrongLabel
        var weakLabel = fullWeakLabel

        fun labelsFit(): Boolean {
            val combinedWidth = labelPaint.measureText(strongLabel) + labelPaint.measureText(weakLabel) + minGap
            return combinedWidth <= availableWidth
        }

        while (!labelsFit() && labelPaint.textSize > minLabelTextSize) {
            labelPaint.textSize = max(minLabelTextSize, labelPaint.textSize - 0.5f)
        }

        if (!labelsFit()) {
            strongLabel = shortStrongLabel
            weakLabel = shortWeakLabel
            while (!labelsFit() && labelPaint.textSize > minLabelTextSize) {
                labelPaint.textSize = max(minLabelTextSize, labelPaint.textSize - 0.5f)
            }
        }

        val labelsBaseline = barBottom + max(10f, minDimension * 0.019f)
        canvas.drawText(strongLabel, barLeft, labelsBaseline, labelPaint)

        val weakWidth = labelPaint.measureText(weakLabel)
        canvas.drawText(weakLabel, barRight - weakWidth, labelsBaseline, labelPaint)
    }
}
