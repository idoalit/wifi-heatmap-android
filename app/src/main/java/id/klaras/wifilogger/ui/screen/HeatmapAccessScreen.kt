package id.klaras.wifilogger.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.klaras.wifilogger.data.entity.FloorPlan
import id.klaras.wifilogger.viewmodel.FloorPlanViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HeatmapAccessScreen(
    modifier: Modifier = Modifier,
    viewModel: FloorPlanViewModel = koinViewModel(),
    onNavigateToHeatmap: (Long) -> Unit
) {
    val floorPlans by viewModel.floorPlans.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Select a floor plan to open the heatmap.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (floorPlans.isEmpty()) {
            Text(
                text = "No floor plans yet. Add one in Rooms.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(floorPlans) { floorPlan ->
                    FloorPlanHeatmapCard(
                        floorPlan = floorPlan,
                        onOpenHeatmap = { onNavigateToHeatmap(floorPlan.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloorPlanHeatmapCard(
    floorPlan: FloorPlan,
    onOpenHeatmap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenHeatmap),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = floorPlan.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Tap to view heatmap",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open heatmap",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

