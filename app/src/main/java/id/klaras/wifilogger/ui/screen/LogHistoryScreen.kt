package id.klaras.wifilogger.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.klaras.wifilogger.data.entity.WifiLog
import id.klaras.wifilogger.data.entity.WifiLogWithFloorPlan
import id.klaras.wifilogger.viewmodel.LogHistoryViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogHistoryScreen(
    viewModel: LogHistoryViewModel = koinViewModel(),
    onNavigateToHeatmap: (String) -> Unit
) {
    val context = LocalContext.current
    val logs by viewModel.logsWithFloorPlan.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    val expandedFloorPlans = remember { mutableStateMapOf<String, Boolean>() }
    val groupedLogs = viewModel.getLogsGroupedByFloorPlanWithId()

    // Handle export result
    LaunchedEffect(exportResult) {
        exportResult?.let { result ->
            when (result) {
                is LogHistoryViewModel.ExportResult.Success -> {
                    Toast.makeText(
                        context,
                        "Berhasil diekspor ke ${result.filePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is LogHistoryViewModel.ExportResult.Error -> {
                    Toast.makeText(
                        context,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            viewModel.clearExportResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Riwayat Log WiFi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { viewModel.exportToCsv(context) },
                enabled = logs.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summary
        Text(
            text = "Total: ${logs.size} log dari ${groupedLogs.size} denah",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada log WiFi",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedLogs.forEach { (floorPlanKey, floorPlanLogs) ->
                    val (floorPlanId, floorPlanName) = floorPlanKey
                    item(key = "header_$floorPlanName") {
                        FloorPlanLogHeader(
                            floorPlanName = floorPlanName,
                            logCount = floorPlanLogs.size,
                            isExpanded = expandedFloorPlans[floorPlanName] ?: true,
                            isLoading = isLoading,
                            onToggle = {
                                expandedFloorPlans[floorPlanName] = !(expandedFloorPlans[floorPlanName] ?: true)
                            },
                            onExport = {
                                viewModel.exportFloorPlanToCsv(context, floorPlanId, floorPlanName)
                            },
                            onGenerateHeatmap = {
                                onNavigateToHeatmap(floorPlanId.toString())
                            }
                        )
                    }

                    if (expandedFloorPlans[floorPlanName] != false) {
                        items(
                            items = floorPlanLogs,
                            key = { it.wifiLog.id }
                        ) { log ->
                            LogItemCard(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloorPlanLogHeader(
    floorPlanName: String,
    logCount: Int,
    isExpanded: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit,
    onExport: () -> Unit,
    onGenerateHeatmap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = floorPlanName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$logCount log",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Row {
                IconButton(
                    onClick = onGenerateHeatmap,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Generate Heatmap",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(
                    onClick = onExport,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Tutup" else "Buka",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun LogItemCard(log: WifiLogWithFloorPlan) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = getSignalColor(log.wifiLog.rssi),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.wifiLog.ssid.ifEmpty { "<Hidden>" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "BSSID: ${log.wifiLog.bssid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Koordinat: (${String.format("%.1f", log.wifiLog.coordinateX)}, ${String.format("%.1f", log.wifiLog.coordinateY)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = dateFormat.format(Date(log.wifiLog.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${log.wifiLog.rssi} dBm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = getSignalColor(log.wifiLog.rssi)
                )
                Text(
                    text = "${log.wifiLog.frequency} MHz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getSignalColor(rssi: Int) = when {
    rssi >= -50 -> MaterialTheme.colorScheme.primary
    rssi >= -70 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

@Preview(showBackground = true)
@Composable
private fun LogItemCardPreview() {
    MaterialTheme {
        LogItemCard(
            log = WifiLogWithFloorPlan(
                wifiLog = WifiLog(
                    id = 1,
                    floorPlanId = 1,
                    coordinateX = 150.5f,
                    coordinateY = 200.3f,
                    ssid = "WiFi Network",
                    bssid = "00:11:22:33:44:55",
                    rssi = -55,
                    frequency = 2437,
                    timestamp = System.currentTimeMillis()
                ),
                floorPlanName = "Lantai 1"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FloorPlanLogHeaderPreview() {
    MaterialTheme {
        FloorPlanLogHeader(
            floorPlanName = "Lantai 1",
            logCount = 15,
            isExpanded = true,
            isLoading = false,
            onToggle = {},
            onExport = {},
            onGenerateHeatmap = {}
        )
    }
}