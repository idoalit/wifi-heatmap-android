package id.klaras.wifilogger.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.data.entity.RouterPoint
import id.klaras.wifilogger.viewmodel.FloorPlanViewModel
import id.klaras.wifilogger.viewmodel.WifiLogViewModel
import id.klaras.wifilogger.viewmodel.WifiScanUiState
import id.klaras.wifilogger.viewmodel.WifiScannerViewModel
import id.klaras.wifilogger.wifi.WifiScanResult
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoggingScreen(
    floorPlanViewModel: FloorPlanViewModel = koinViewModel(),
    wifiScannerViewModel: WifiScannerViewModel = koinViewModel(),
    wifiLogViewModel: WifiLogViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Floor plans data
    val floorPlans by floorPlanViewModel.floorPlans.collectAsState()
    val routerPoints by floorPlanViewModel.routerPoints.collectAsState()
    var selectedFloorPlan by remember { mutableStateOf<FloorPlan?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    // Sampling position
    var samplingX by remember { mutableFloatStateOf(-1f) }
    var samplingY by remember { mutableFloatStateOf(-1f) }
    val hasSamplingPosition = samplingX >= 0 && samplingY >= 0
    
    // Scan state
    val scanState by wifiScannerViewModel.scanState.collectAsState()
    val scanResults by wifiScannerViewModel.scanResults.collectAsState()
    
    // Logging state
    var isLogging by remember { mutableStateOf(false) }
    var savedCount by remember { mutableStateOf(0) }
    var lastLogTime by remember { mutableStateOf<Long?>(null) }
    
    // Permission state
    var hasPermission by remember { mutableStateOf(false) }
    
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
    
    LaunchedEffect(Unit) {
        hasPermission = requiredPermissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // Load router points when floor plan is selected
    LaunchedEffect(selectedFloorPlan) {
        selectedFloorPlan?.let {
            floorPlanViewModel.selectFloorPlan(it)
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
        if (hasPermission && selectedFloorPlan != null && hasSamplingPosition) {
            isLogging = true
            wifiScannerViewModel.startScan()
        }
    }
    
    // Handle scan completion - save results to database
    LaunchedEffect(scanState) {
        if (scanState is WifiScanUiState.Success && isLogging && selectedFloorPlan != null && hasSamplingPosition) {
            // Save all scan results
            wifiScannerViewModel.saveAllResultsToFloorPlan(
                floorPlanId = selectedFloorPlan!!.id,
                coordinateX = samplingX,
                coordinateY = samplingY
            )
            
            savedCount = scanResults.size
            lastLogTime = System.currentTimeMillis()
            
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Saved ${scanResults.size} WiFi networks at (${String.format("%.1f", samplingX)}, ${String.format("%.1f", samplingY)})"
                )
            }
            
            // Refresh scan points
            wifiLogViewModel.loadScanPoints(selectedFloorPlan!!.id)
            isLogging = false
        } else if (scanState is WifiScanUiState.Error && isLogging) {
            isLogging = false
            scope.launch {
                snackbarHostState.showSnackbar(
                    (scanState as WifiScanUiState.Error).message
                )
            }
        }
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "WiFi Logging",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Select a floor plan, tap on the map to mark your position, then scan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Floor Plan Selector
            Text(
                text = "Select Floor Plan",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (floorPlans.isEmpty()) {
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
                            text = "No floor plans available. Please create a floor plan in the Rooms menu first.",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                // Dropdown for selecting floor plan
                Box {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (selectedFloorPlan != null) {
                                    Text(
                                        text = selectedFloorPlan!!.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${routerPoints.size} router(s) marked",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Choose a floor plan...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                        floorPlans.forEach { floorPlan ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = floorPlan.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    selectedFloorPlan = floorPlan
                                    samplingX = -1f
                                    samplingY = -1f
                                    dropdownExpanded = false
                                },
                                trailingIcon = {
                                    if (selectedFloorPlan?.id == floorPlan.id) {
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
            }
            
            // Floor plan with sampling position
            if (selectedFloorPlan != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tap on the map to mark your current position:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                FloorPlanWithSampling(
                    floorPlan = selectedFloorPlan!!,
                    routerPoints = routerPoints,
                    samplingX = samplingX,
                    samplingY = samplingY,
                    onSamplingPositionChanged = { x, y ->
                        samplingX = x
                        samplingY = y
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                )
                
                if (hasSamplingPosition) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sampling position: (${String.format("%.1f", samplingX)}, ${String.format("%.1f", samplingY)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start Scan Button
            Button(
                onClick = {
                    if (!hasPermission) {
                        permissionLauncher.launch(requiredPermissions)
                    } else {
                        isLogging = true
                        wifiScannerViewModel.startScan()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedFloorPlan != null && 
                          hasSamplingPosition &&
                          scanState !is WifiScanUiState.Scanning &&
                          !isLogging &&
                          wifiScannerViewModel.isWifiEnabled()
            ) {
                if (scanState is WifiScanUiState.Scanning || isLogging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan WiFi")
                }
            }
            
            // WiFi status warning
            if (!wifiScannerViewModel.isWifiEnabled()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WiFi is disabled. Please enable WiFi to start scanning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Last Log Info
            if (lastLogTime != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Last Log",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Networks saved:",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$savedCount",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Time:",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = formatTimestamp(lastLogTime!!),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Current scan results
            if (scanResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Last Scan Results (${scanResults.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scanResults.take(5).forEach { result ->
                        LoggedNetworkItem(result = result)
                    }
                    if (scanResults.size > 5) {
                        Text(
                            text = "... and ${scanResults.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloorPlanWithSampling(
    floorPlan: FloorPlan,
    routerPoints: List<RouterPoint>,
    samplingX: Float,
    samplingY: Float,
    onSamplingPositionChanged: (Float, Float) -> Unit,
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
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (imageSize.width > 0 && imageSize.height > 0) {
                            val relativeX = (offset.x / imageSize.width) * 100f
                            val relativeY = (offset.y / imageSize.height) * 100f
                            onSamplingPositionChanged(relativeX, relativeY)
                        }
                    }
                }
        )
        
        // Draw router points (gray, for reference)
        routerPoints.forEachIndexed { index, point ->
            if (imageSize.width > 0 && imageSize.height > 0) {
                val pinX = (point.coordinateX / 100f) * imageSize.width
                val pinY = (point.coordinateY / 100f) * imageSize.height
                
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pinX - 10.dp.toPx()).toInt(),
                                y = (pinY - 10.dp.toPx()).toInt()
                            )
                        }
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "R",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
        
        // Draw sampling position (blue pin)
        if (samplingX >= 0 && samplingY >= 0 && imageSize.width > 0 && imageSize.height > 0) {
            val pinX = (samplingX / 100f) * imageSize.width
            val pinY = (samplingY / 100f) * imageSize.height
            
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Sampling Position",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(36.dp)
                    .offset {
                        IntOffset(
                            x = (pinX - 18.dp.toPx()).toInt(),
                            y = (pinY - 36.dp.toPx()).toInt()
                        )
                    }
            )
        }
    }
}

@Composable
fun LoggedNetworkItem(
    result: WifiScanResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getSignalColor(result.getSignalStrengthPercent()).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${result.rssi}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = getSignalColor(result.getSignalStrengthPercent())
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.ssid,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${result.getBand()} • ${result.getSignalQuality()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "dBm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getSignalColor(strength: Int): Color {
    return when {
        strength >= 80 -> Color(0xFF4CAF50)
        strength >= 60 -> Color(0xFF8BC34A)
        strength >= 40 -> Color(0xFFFFC107)
        strength >= 20 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
