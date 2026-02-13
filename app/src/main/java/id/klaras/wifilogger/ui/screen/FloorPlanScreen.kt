package id.klaras.wifilogger.ui.screen

import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.data.entity.RouterPoint
import id.klaras.wifilogger.viewmodel.FloorPlanViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream

sealed class FloorPlanScreenState {
    data object List : FloorPlanScreenState()
    data object AddForm : FloorPlanScreenState()
    data class EditRouters(val floorPlanId: Long) : FloorPlanScreenState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanScreen(
    viewModel: FloorPlanViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val floorPlans by viewModel.floorPlans.collectAsState()
    val routerPoints by viewModel.routerPoints.collectAsState()
    val selectedFloorPlan by viewModel.selectedFloorPlan.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var screenState by remember { mutableStateOf<FloorPlanScreenState>(FloorPlanScreenState.List) }
    var showDeleteDialog by remember { mutableStateOf<FloorPlan?>(null) }
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    when (val state = screenState) {
        is FloorPlanScreenState.List -> {
            Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { screenState = FloorPlanScreenState.AddForm }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Floor Plan")
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {

                    if (floorPlans.isEmpty()) {
                        Text(
                            text = "No floor plans yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(floorPlans) { floorPlan ->
                                FloorPlanCard(
                                    floorPlan = floorPlan,
                                    onEditRouters = {
                                        viewModel.selectFloorPlan(floorPlan)
                                        screenState = FloorPlanScreenState.EditRouters(floorPlan.id)
                                    },
                                    onDelete = { showDeleteDialog = floorPlan }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        is FloorPlanScreenState.AddForm -> {
            Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    FloorPlanForm(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        onSave = { name, imagePath ->
                            viewModel.insertFloorPlan(name, imagePath) { newId ->
                                screenState = FloorPlanScreenState.List
                                scope.launch {
                                    snackbarHostState.showSnackbar("Floor plan saved!")
                                }
                            }
                        },
                        onCancel = { screenState = FloorPlanScreenState.List }
                    )
                }
            }
        }
        
        is FloorPlanScreenState.EditRouters -> {
            selectedFloorPlan?.let { floorPlan ->
                Scaffold(
                    modifier = modifier,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { screenState = FloorPlanScreenState.List }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = floorPlan.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        RouterPointEditor(
                            floorPlan = floorPlan,
                            routerPoints = routerPoints,
                            onAddRouterPoint = { x, y ->
                                viewModel.addRouterPoint(floorPlan.id, x, y)
                            },
                            onDeleteRouterPoint = { routerPoint ->
                                viewModel.deleteRouterPoint(routerPoint)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { floorPlan ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Floor Plan") },
            text = { Text("Are you sure you want to delete \"${floorPlan.name}\"? This will also delete all associated router points and WiFi logs.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFloorPlan(floorPlan)
                        showDeleteDialog = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Floor plan deleted")
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FloorPlanForm(
    onSave: (name: String, imagePath: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var floorPlanName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var savedImagePath by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Copy image to app's internal storage
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val fileName = "floor_plan_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)
                inputStream?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                savedImagePath = file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = floorPlanName,
            onValueChange = { floorPlanName = it },
            label = { Text("Floor Plan Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (selectedImageUri == null) "Select Floor Plan Image" else "Change Image")
        }
        
        if (selectedImageUri != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(selectedImageUri)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Floor Plan Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.medium
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    savedImagePath?.let { path ->
                        onSave(floorPlanName, path)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = floorPlanName.isNotBlank() && savedImagePath != null
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun FloorPlanCard(
    floorPlan: FloorPlan,
    onEditRouters: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditRouters() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(File(floorPlan.imagePath))
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Floor plan thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = floorPlan.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Tap to edit router positions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun RouterPointEditor(
    floorPlan: FloorPlan,
    routerPoints: List<RouterPoint>,
    onAddRouterPoint: (x: Float, y: Float) -> Unit,
    onDeleteRouterPoint: (RouterPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedRouter by remember { mutableStateOf<RouterPoint?>(null) }
    
    Column(modifier = modifier) {
        Text(
            text = "Router Positions",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tap on the floor plan to add router positions. These are reference markers for where routers/access points are located.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = 1.dp,
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
                                onAddRouterPoint(relativeX, relativeY)
                            }
                        }
                    }
            )
            
            // Draw all router points
            routerPoints.forEachIndexed { index, point ->
                if (imageSize.width > 0 && imageSize.height > 0) {
                    val pinX = (point.coordinateX / 100f) * imageSize.width
                    val pinY = (point.coordinateY / 100f) * imageSize.height
                    
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (pinX - 12.dp.toPx()).toInt(),
                                    y = (pinY - 12.dp.toPx()).toInt()
                                )
                            }
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedRouter == point) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            .clickable { selectedRouter = if (selectedRouter == point) null else point },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Routers: ${routerPoints.size}",
            style = MaterialTheme.typography.titleSmall
        )
        
        // Router list
        if (routerPoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                routerPoints.forEachIndexed { index, point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedRouter == point) 
                                    MaterialTheme.colorScheme.errorContainer
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Router ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "(${String.format("%.1f", point.coordinateX)}, ${String.format("%.1f", point.coordinateY)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { onDeleteRouterPoint(point) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FloorPlanCardPreview() {
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            FloorPlanCard(
                floorPlan = FloorPlan(
                    id = 1,
                    name = "Lantai 1 - Office",
                    imagePath = "/path/to/image.jpg"
                ),
                onEditRouters = {},
                onDelete = {}
            )
            FloorPlanCard(
                floorPlan = FloorPlan(
                    id = 2,
                    name = "Lantai 2 - Meeting Rooms",
                    imagePath = "/path/to/image2.jpg"
                ),
                onEditRouters = {},
                onDelete = {}
            )
        }
    }
}
