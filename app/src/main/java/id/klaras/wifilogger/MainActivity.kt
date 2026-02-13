package id.klaras.wifilogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import id.klaras.wifilogger.auth.AuthState
import id.klaras.wifilogger.auth.AuthViewModel
import id.klaras.wifilogger.ui.screen.FloorPlanScreen
import id.klaras.wifilogger.ui.screen.HeatmapAccessScreen
import id.klaras.wifilogger.ui.screen.HeatmapScreen
import id.klaras.wifilogger.ui.screen.LogHistoryScreen
import id.klaras.wifilogger.ui.screen.LoggingScreen
import id.klaras.wifilogger.ui.screen.LoginScreen
import id.klaras.wifilogger.ui.screen.WifiScanScreen
import id.klaras.wifilogger.ui.theme.WiFiLoggerTheme
import org.koin.androidx.compose.koinViewModel

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
fun WiFiLoggerApp(
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    when (authState) {
        is AuthState.SignedIn -> {
            AuthedApp(
                onSignOut = {
                    GoogleSignIn.getClient(
                        context,
                        GoogleSignInOptions.DEFAULT_SIGN_IN
                    ).signOut()
                    authViewModel.signOut()
                }
            )
        }
        is AuthState.SignedOut -> {
            LoginScreen(
                isBusy = authViewModel.isBusy.collectAsState().value,
                errorMessage = authViewModel.errorMessage.collectAsState().value,
                onGoogleIdToken = { authViewModel.signInWithGoogle(it) },
                onDismissError = { authViewModel.clearError() }
            )
        }
        AuthState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthedApp(
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationSuiteScaffold(
        modifier = Modifier.fillMaxSize(),
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(when (currentRoute?.substringBefore("/")) {
                            AppDestinations.FLOOR_PLANS.name -> "Floor Plans"
                            AppDestinations.WIFI_SCAN.name -> "Heatmap"
                            AppDestinations.LOGGING.name -> "WiFi Logging"
                            AppDestinations.LOG_HISTORY.name -> "Riwayat Log WiFi"
                            "heatmap" -> "WiFi Heatmap"
                            else -> "WiFi Logger"
                        })
                    },
                    actions = {
                        IconButton(onClick = onSignOut) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign out")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestinations.FLOOR_PLANS.name,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(AppDestinations.FLOOR_PLANS.name) {
                    FloorPlanScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
                composable(AppDestinations.LOGGING.name) {
                    LoggingScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
                composable(AppDestinations.WIFI_SCAN.name) {
                    HeatmapAccessScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onNavigateToHeatmap = { floorPlanId ->
                            navController.navigate("heatmap/$floorPlanId")
                        }
                    )
                }
                composable(AppDestinations.LOG_HISTORY.name) {
                    LogHistoryScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onNavigateToHeatmap = {
                            navController.navigate("heatmap/$it")
                        }
                    )
                }
                composable(
                    route = "heatmap/{floorPlanId}",
                    arguments = listOf(navArgument("floorPlanId") { type = NavType.StringType })
                ) {
                    HeatmapScreen(
                        floorPlanId = it.arguments?.getString("floorPlanId") ?: "",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    FLOOR_PLANS("Rooms", Icons.Default.Home),
    WIFI_SCAN("Heatmap", Icons.Default.Map),
    LOGGING("Logging", Icons.Default.Create),
    LOG_HISTORY("Logs", Icons.Default.List),
}
