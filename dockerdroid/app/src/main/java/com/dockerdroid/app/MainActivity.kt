package com.dockerdroid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dockerdroid.app.core.service.DockerForegroundService
import com.dockerdroid.app.ui.navigation.Destination
import com.dockerdroid.app.ui.screens.ComposeScreen
import com.dockerdroid.app.ui.screens.ContainersScreen
import com.dockerdroid.app.ui.screens.DashboardScreen
import com.dockerdroid.app.ui.screens.ImagesScreen
import com.dockerdroid.app.ui.screens.InstallScreen
import com.dockerdroid.app.ui.screens.SettingsScreen
import com.dockerdroid.app.ui.screens.TerminalScreen
import com.dockerdroid.app.ui.screens.WelcomeScreen
import com.dockerdroid.app.ui.theme.DockerDroidTheme
import com.dockerdroid.app.ui.viewmodel.ComposeViewModel
import com.dockerdroid.app.ui.viewmodel.ContainersViewModel
import com.dockerdroid.app.ui.viewmodel.DashboardViewModel
import com.dockerdroid.app.ui.viewmodel.ImagesViewModel
import com.dockerdroid.app.ui.viewmodel.InstallViewModel
import com.dockerdroid.app.ui.viewmodel.SettingsViewModel
import com.dockerdroid.app.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DockerDroidApplication).container
        val factory = ViewModelFactory(container)

        setContent {
            DockerDroidTheme {
                DockerDroidApp(
                    factory = factory,
                    onStartService = { DockerForegroundService.start(this) },
                    readUri = { uri ->
                        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    },
                )
            }
        }
    }
}

@Composable
private fun DockerDroidApp(
    factory: ViewModelFactory,
    onStartService: () -> Unit,
    readUri: (android.net.Uri) -> String?,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = Destination.bottomBar.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Destination.bottomBar.forEach { dest ->
                        NavigationBarItem(
                            selected = backStack?.destination?.hierarchy?.any { it.route == dest.route } == true,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Welcome.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Welcome.route) {
                WelcomeScreen(onContinue = { navController.navigate(Destination.Install.route) })
            }
            composable(Destination.Install.route) {
                val vm: InstallViewModel = viewModel(factory = factory)
                InstallScreen(vm, onInstalled = {
                    onStartService()
                    navController.navigate(Destination.Dashboard.route) {
                        popUpTo(Destination.Welcome.route) { inclusive = true }
                    }
                })
            }
            composable(Destination.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(vm)
            }
            composable(Destination.Containers.route) {
                val vm: ContainersViewModel = viewModel(factory = factory)
                ContainersScreen(vm)
            }
            composable(Destination.Images.route) {
                val vm: ImagesViewModel = viewModel(factory = factory)
                ImagesScreen(vm)
            }
            composable(Destination.Compose.route) {
                val vm: ComposeViewModel = viewModel(factory = factory)
                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    uri?.let { readUri(it)?.let { yaml -> vm.import(it.lastPathSegment ?: "stack", yaml) } }
                }
                ComposeScreen(vm, onImport = { picker.launch(arrayOf("*/*")) })
            }
            composable(Destination.Terminal.route) {
                TerminalScreen(shell = (navController.context.applicationContext as DockerDroidApplication).container.shell)
            }
            composable(Destination.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(vm)
            }
        }
    }
}
