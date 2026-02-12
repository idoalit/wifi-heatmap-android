package id.klaras.wifilogger.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import id.klaras.wifilogger.data.dao.HeatmapPoint
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.viewmodel.HeatmapUiState
import id.klaras.wifilogger.viewmodel.HeatmapViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun HeatmapScreen(
    floorPlanId: String,
    viewModel: HeatmapViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(floorPlanId) {
        viewModel.loadHeatmap(floorPlanId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "WiFi Heatmap",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = uiState) {
            is HeatmapUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HeatmapUiState.Error -> {
                ErrorCard(message = state.message)
            }

            is HeatmapUiState.NoData -> {
                NoDataCard()
            }

            is HeatmapUiState.Success -> {
                HeatmapContent(
                    floorPlan = state.floorPlan,
                    heatmapPoints = state.heatmapPoints,
                    availableSsids = state.availableSsids,
                    selectedSsid = state.selectedSsid,
                    onSsidSelected = { viewModel.selectSsid(it) }
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun NoDataCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No WiFi data available",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please scan some WiFi networks on this floor plan first using the Logging feature.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun HeatmapContent(
    floorPlan: FloorPlan,
    heatmapPoints: List<HeatmapPoint>,
    availableSsids: List<String>,
    selectedSsid: String?,
    onSsidSelected: (String?) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Text(
        text = floorPlan.name,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    // SSID Selector
    if (availableSsids.isNotEmpty()) {
        Text(
            text = "Filter by Network",
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dropdownExpanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedSsid ?: "All Networks (${availableSsids.size})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Expand"
                    )
                }
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "All Networks",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = {
                        onSsidSelected(null)
                        dropdownExpanded = false
                    },
                    trailingIcon = {
                        if (selectedSsid == null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                availableSsids.forEach { ssid ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = ssid,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = {
                            onSsidSelected(ssid)
                            dropdownExpanded = false
                        },
                        trailingIcon = {
                            if (selectedSsid == ssid) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Heatmap visualization
    HeatmapVisualization(
        floorPlan = floorPlan,
        heatmapPoints = heatmapPoints,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Legend
    SignalLegend()

    Spacer(modifier = Modifier.height(16.dp))

    // Statistics
    HeatmapStatistics(heatmapPoints = heatmapPoints)
}

@Composable
private fun HeatmapVisualization(
    floorPlan: FloorPlan,
    heatmapPoints: List<HeatmapPoint>,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        // Floor plan image
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(File(floorPlan.imagePath))
                    .crossfade(true)
                    .build()
            ),
            contentDescription = "Floor Plan",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { imageSize = it }
        )

        // Heatmap overlay
        if (imageSize.width > 0 && imageSize.height > 0 && heatmapPoints.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw heatmap circles with interpolation
                heatmapPoints.forEach { point ->
                    val centerX = (point.coordinateX / 100f) * width
                    val centerY = (point.coordinateY / 100f) * height
                    val color = rssiToColor(point.avgRssi.toFloat())

                    // Draw multiple circles with decreasing opacity for a gradient effect
                    val baseRadius = minOf(width, height) * 0.12f
                    for (i in 5 downTo 1) {
                        val radius = baseRadius * (i / 5f)
                        val alpha = 0.15f * (6 - i) / 5f
                        drawCircle(
                            color = color.copy(alpha = alpha),
                            radius = radius,
                            center = Offset(centerX, centerY)
                        )
                    }

                    // Draw center point
                    drawCircle(
                        color = color,
                        radius = 8f,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Signal Strength",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00C853), // Strong (green)
                                    Color(0xFFFFEB3B), // Medium (yellow)
                                    Color(0xFFFF9800), // Weak (orange)
                                    Color(0xFFF44336)  // Very weak (red)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Strong (-30 dBm)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Weak (-100 dBm)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeatmapStatistics(heatmapPoints: List<HeatmapPoint>) {
    val totalScans = heatmapPoints.sumOf { it.scanCount }
    val avgRssi = heatmapPoints.map { it.avgRssi }.average()
    val minRssi = heatmapPoints.minOfOrNull { it.minRssi } ?: 0
    val maxRssi = heatmapPoints.maxOfOrNull { it.maxRssi } ?: 0

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Scan Points", value = "${heatmapPoints.size}")
                StatItem(label = "Total Scans", value = "$totalScans")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Avg Signal", value = "${String.format("%.1f", avgRssi)} dBm")
                StatItem(label = "Best Signal", value = "$maxRssi dBm")
                StatItem(label = "Worst Signal", value = "$minRssi dBm")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Convert RSSI value to a color on the gradient from green (strong) to red (weak)
 * RSSI typically ranges from -30 dBm (strong) to -100 dBm (weak)
 */
private fun rssiToColor(rssi: Float): Color {
    // Normalize RSSI to 0-1 range (where 0 is weak and 1 is strong)
    val normalized = ((rssi + 100) / 70f).coerceIn(0f, 1f)

    return when {
        normalized > 0.75f -> Color(0xFF00C853) // Strong - Green
        normalized > 0.5f -> Color(0xFFFFEB3B)  // Good - Yellow
        normalized > 0.25f -> Color(0xFFFF9800) // Fair - Orange
        else -> Color(0xFFF44336)               // Weak - Red
    }
}

