package com.saeed.mirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saeed.mirror.data.AppViewModel
import com.saeed.mirror.ui.capture.CaptureScreen
import com.saeed.mirror.ui.home.HomeScreen
import com.saeed.mirror.ui.result.ResultScreen
import com.saeed.mirror.ui.saved.SavedScreen
import com.saeed.mirror.ui.styles.StylesScreen
import com.saeed.mirror.ui.theme.MirrorColors
import com.saeed.mirror.ui.theme.MirrorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MirrorTheme { MirrorApp() }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val STYLES = "styles"
    const val SAVED = "saved"
    const val CAPTURE = "capture"
    const val TRY_ON = "tryon/{styleId}"
    const val LOOK = "look/{lookId}"
    fun tryOn(styleId: String) = "tryon/$styleId"
    fun look(lookId: String) = "look/$lookId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home),
    Tab(Routes.STYLES, "Styles", Icons.Outlined.ContentCut),
    Tab(Routes.SAVED, "Saved", Icons.Outlined.FavoriteBorder)
)

fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MirrorApp() {
    val nav = rememberNavController()
    val model: AppViewModel = viewModel()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showTabs = tabs.any { it.route == route }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            if (showTabs) {
                NavigationBar(containerColor = Color(0xFF161618)) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MirrorColors.Accent,
                                selectedTextColor = MirrorColors.Accent,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MirrorColors.Secondary,
                                unselectedTextColor = MirrorColors.Secondary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    model = model,
                    onScan = { nav.navigate(Routes.CAPTURE) },
                    onOpenLook = { nav.navigate(Routes.look(it.id)) },
                    onSeeAll = { nav.switchTab(Routes.SAVED) }
                )
            }
            composable(Routes.STYLES) {
                StylesScreen(
                    model = model,
                    onTryOn = { nav.navigate(Routes.tryOn(it.id)) },
                    onScan = { nav.navigate(Routes.CAPTURE) }
                )
            }
            composable(Routes.SAVED) {
                SavedScreen(
                    model = model,
                    onOpenLook = { nav.navigate(Routes.look(it.id)) },
                    onBrowse = { nav.switchTab(Routes.STYLES) }
                )
            }
            composable(Routes.CAPTURE) {
                CaptureScreen(
                    model = model,
                    onClose = { nav.popBackStack() },
                    onFinished = {
                        nav.popBackStack()
                        nav.switchTab(Routes.STYLES)
                    }
                )
            }
            composable(Routes.TRY_ON, arguments = listOf(navArgument("styleId") { type = NavType.StringType })) { entry ->
                ResultScreen(
                    model = model,
                    styleId = entry.arguments?.getString("styleId").orEmpty(),
                    lookId = null,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.LOOK, arguments = listOf(navArgument("lookId") { type = NavType.StringType })) { entry ->
                val look = model.look(entry.arguments?.getString("lookId").orEmpty())
                ResultScreen(
                    model = model,
                    styleId = look?.styleId.orEmpty(),
                    lookId = look?.id,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
