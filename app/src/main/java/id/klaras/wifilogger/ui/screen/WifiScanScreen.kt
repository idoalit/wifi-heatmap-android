package id.klaras.wifilogger.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import id.klaras.wifilogger.viewmodel.WifiScanUiState
import id.klaras.wifilogger.viewmodel.WifiScannerViewModel
import id.klaras.wifilogger.wifi.WifiScanResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WifiScanScreen(
    viewModel: WifiScannerViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val scanState by viewModel.scanState.collectAsState()
    val scanResults by viewModel.scanResults.collectAsState()
    val selectedNetworks by viewModel.selectedNetworks.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()
    
    var hasLocationPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    
    // Required permissions for WiFi scanning
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
    
    // Check initial permission state
    LaunchedEffect(Unit) {
        hasLocationPermission = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        // Load cached results if available
        if (hasLocationPermission) {
            viewModel.getCachedResults()
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
        permissionDenied = !hasLocationPermission
        
        if (hasLocationPermission) {
            viewModel.startScan()
        }
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // Permission Status
            if (!hasLocationPermission) {
                PermissionRequestCard(
                    permissionDenied = permissionDenied,
                    onRequestPermission = {
                        permissionLauncher.launch(requiredPermissions)
                    }
                )
            } else {
                // Scan Controls
                ScanControlCard(
                    scanState = scanState,
                    lastScanTime = lastScanTime,
                    onScan = { viewModel.startScan() },
                    isWifiEnabled = viewModel.isWifiEnabled()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Scan Results
            if (hasLocationPermission && scanResults.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Networks Found: ${scanResults.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (selectedNetworks.isNotEmpty()) {
                        Text(
                            text = "${selectedNetworks.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(scanResults) { network ->
                        WifiNetworkCard(
                            network = network,
                            isSelected = selectedNetworks.contains(network),
                            onClick = { viewModel.toggleNetworkSelection(network) }
                        )
                    }
                }
            } else if (hasLocationPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when (scanState) {
                        is WifiScanUiState.Scanning -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Scanning for networks...")
                            }
                        }
                        is WifiScanUiState.Error -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = (scanState as WifiScanUiState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "Tap 'Scan' to find nearby WiFi networks",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequestCard(
    permissionDenied: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (permissionDenied) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (permissionDenied) "Permission Required" else "Location Permission Needed",
                style = MaterialTheme.typography.titleMedium,
                color = if (permissionDenied)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "WiFi scanning requires location permission on Android 10+. " +
                        "This is needed to detect nearby WiFi networks.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (permissionDenied)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun ScanControlCard(
    scanState: WifiScanUiState,
    lastScanTime: Long?,
    onScan: () -> Unit,
    isWifiEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var cooldownRemaining by remember { mutableStateOf(0L) }
    
    // Handle cooldown countdown
    LaunchedEffect(scanState) {
        if (scanState is WifiScanUiState.Cooldown) {
            cooldownRemaining = scanState.remainingMs
            while (cooldownRemaining > 0) {
                delay(1000)
                cooldownRemaining -= 1000
            }
        } else if (scanState is WifiScanUiState.Throttled) {
            cooldownRemaining = scanState.retryAfterMs
            while (cooldownRemaining > 0) {
                delay(1000)
                cooldownRemaining -= 1000
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WiFi Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isWifiEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isWifiEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
                
                Button(
                    onClick = onScan,
                    enabled = scanState !is WifiScanUiState.Scanning && 
                              isWifiEnabled && 
                              cooldownRemaining <= 0
                ) {
                    if (scanState is WifiScanUiState.Scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (cooldownRemaining > 0) "Wait ${cooldownRemaining / 1000}s" else "Scan")
                }
            }
            
            // Throttle warning
            AnimatedVisibility(visible = scanState is WifiScanUiState.Throttled) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (scanState as? WifiScanUiState.Throttled)?.message 
                                    ?: "Scan throttled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Last scan time
            if (lastScanTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last scan: ${formatTime(lastScanTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Cached results indicator
            if (scanState is WifiScanUiState.Success && scanState.isCached) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Showing cached results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun WifiNetworkCard(
    network: WifiScanResult,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal strength indicator
            SignalStrengthIndicator(
                strength = network.getSignalStrengthPercent(),
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = network.ssid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${network.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = network.getBand(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = network.getSignalQuality(),
                        style = MaterialTheme.typography.bodySmall,
                        color = getSignalColor(network.getSignalStrengthPercent())
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SignalStrengthIndicator(
    strength: Int,
    modifier: Modifier = Modifier
) {
    val color = getSignalColor(strength)
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { strength / 100f },
            modifier = Modifier.size(36.dp),
            color = color,
            strokeWidth = 4.dp,
            trackColor = color.copy(alpha = 0.1f)
        )
        Text(
            text = "$strength",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun getSignalColor(strength: Int): Color {
    return when {
        strength >= 80 -> Color(0xFF4CAF50) // Green
        strength >= 60 -> Color(0xFF8BC34A) // Light Green
        strength >= 40 -> Color(0xFFFFC107) // Amber
        strength >= 20 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
private fun SignalStrengthIndicatorPreview() {
    MaterialTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            SignalStrengthIndicator(strength = 90)
            SignalStrengthIndicator(strength = 70)
            SignalStrengthIndicator(strength = 50)
            SignalStrengthIndicator(strength = 30)
            SignalStrengthIndicator(strength = 10)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WifiNetworkItemPreview() {
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WifiNetworkCard(
                network = WifiScanResult(
                    ssid = "My WiFi Network",
                    bssid = "00:11:22:33:44:55",
                    rssi = -55,
                    frequency = 2437,
                    capabilities = "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"
                ),
                isSelected = false,
                onClick = {}
            )
            WifiNetworkCard(
                network = WifiScanResult(
                    ssid = "Office WiFi 5GHz",
                    bssid = "AA:BB:CC:DD:EE:FF",
                    rssi = -72,
                    frequency = 5180,
                    capabilities = "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"
                ),
                isSelected = true,
                onClick = {}
            )
        }
    }
}
