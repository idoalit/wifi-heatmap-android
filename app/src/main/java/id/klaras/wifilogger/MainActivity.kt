package id.klaras.wifilogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.klaras.wifilogger.ui.screen.FloorPlanScreen
import id.klaras.wifilogger.ui.screen.HeatmapScreen
import id.klaras.wifilogger.ui.screen.LogHistoryScreen
import id.klaras.wifilogger.ui.screen.LoggingScreen
import id.klaras.wifilogger.ui.screen.WifiScanScreen
import id.klaras.wifilogger.ui.theme.WiFiLoggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WiFiLoggerTheme {
                WiFiLoggerApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun WiFiLoggerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it.name == currentRoute,
                    onClick = { navController.navigate(it.name) }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = AppDestinations.FLOOR_PLANS.name
        ) {
            composable(AppDestinations.FLOOR_PLANS.name) {
                FloorPlanScreen(modifier = Modifier.fillMaxSize())
            }
            composable(AppDestinations.LOGGING.name) {
                LoggingScreen(modifier = Modifier.fillMaxSize())
            }
            composable(AppDestinations.WIFI_SCAN.name) {
                WifiScanScreen(modifier = Modifier.fillMaxSize())
            }
            composable(AppDestinations.LOG_HISTORY.name) {
                LogHistoryScreen(onNavigateToHeatmap = {
                    navController.navigate("heatmap/$it")
                })
            }
            composable(
                route = "heatmap/{floorPlanId}",
                arguments = listOf(navArgument("floorPlanId") { type = NavType.StringType })
            ) {
                HeatmapScreen(floorPlanId = it.arguments?.getString("floorPlanId") ?: "")
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    FLOOR_PLANS("Rooms", Icons.Default.Home),
    WIFI_SCAN("Scan", Icons.Default.Refresh),
    LOGGING("Logging", Icons.Default.Create),
    LOG_HISTORY("History", Icons.Default.List),
}
