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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
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
import com.dockerdroid.app.ui.screens.ContainerDetailScreen
import com.dockerdroid.app.ui.screens.ContainersScreen
import com.dockerdroid.app.ui.screens.DashboardScreen
import com.dockerdroid.app.ui.screens.ImageBuildScreen
import com.dockerdroid.app.ui.screens.ImagesScreen
import com.dockerdroid.app.ui.screens.InstallScreen
import com.dockerdroid.app.ui.screens.NetworksScreen
import com.dockerdroid.app.ui.screens.RemoteConnectScreen
import com.dockerdroid.app.ui.screens.SettingsScreen
import com.dockerdroid.app.ui.screens.VmScreen
import com.dockerdroid.app.ui.screens.TerminalScreen
import com.dockerdroid.app.ui.screens.VolumesScreen
import com.dockerdroid.app.ui.screens.WelcomeScreen
import com.dockerdroid.app.ui.theme.DockerDroidTheme
import com.dockerdroid.app.ui.viewmodel.ComposeViewModel
import com.dockerdroid.app.ui.viewmodel.ContainerDetailViewModel
import com.dockerdroid.app.ui.viewmodel.ContainersViewModel
import com.dockerdroid.app.ui.viewmodel.DashboardViewModel
import com.dockerdroid.app.ui.viewmodel.ImageBuildViewModel
import com.dockerdroid.app.ui.viewmodel.ImagesViewModel
import com.dockerdroid.app.ui.viewmodel.InstallViewModel
import com.dockerdroid.app.ui.viewmodel.NetworksViewModel
import com.dockerdroid.app.ui.viewmodel.RemoteViewModel
import com.dockerdroid.app.ui.viewmodel.SettingsViewModel
import com.dockerdroid.app.ui.viewmodel.VmViewModel
import com.dockerdroid.app.ui.viewmodel.VolumesViewModel
import com.dockerdroid.app.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DockerDroidApplication).container
        val factory = ViewModelFactory(container)

        setContent {
            DockerDroidTheme {
                DockerDroidApp(
                    container = container,
                    factory = factory,
                    onStartService = { DockerForegroundService.start(this) },
                    readUri = { uri ->
                        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    },
                    biometricAuth = { title ->
                        com.dockerdroid.app.core.BiometricGate.authenticate(this, title)
                    },
                    openUrl = { url ->
                        runCatching {
                            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DockerDroidApp(
    container: AppContainer,
    factory: ViewModelFactory,
    onStartService: () -> Unit,
    readUri: (android.net.Uri) -> String?,
    biometricAuth: suspend (String) -> Boolean,
    openUrl: (String) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Auto-reconnect to the most recently used remote host on launch (optionally
    // gated by biometrics); on success, skip onboarding and land on the dashboard.
    LaunchedEffect(Unit) {
        if (container.connectionManager.savedHost() != null) {
            val gate = !container.settingsRepository.requireBiometricOnce() ||
                biometricAuth("Unlock DockerDroid remote host")
            if (gate) {
                container.connectionManager.reconnectSaved().onSuccess {
                    navController.navigate(Destination.Dashboard.route) {
                        popUpTo(Destination.Welcome.route) { inclusive = true }
                    }
                }
            }
        }
    }

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
                WelcomeScreen(
                    onContinue = { navController.navigate(Destination.Install.route) },
                    onConnectRemote = { navController.navigate(Destination.RemoteConnect.route) },
                    onLocalVm = { navController.navigate(Destination.LocalVm.route) },
                )
            }
            composable(Destination.LocalVm.route) {
                val vm: VmViewModel = viewModel(factory = factory)
                VmScreen(vm, onRunning = {
                    navController.navigate(Destination.Dashboard.route) {
                        popUpTo(Destination.Welcome.route) { inclusive = true }
                    }
                })
            }
            composable(Destination.RemoteConnect.route) {
                val vm: RemoteViewModel = viewModel(factory = factory)
                RemoteConnectScreen(vm, onConnected = {
                    // Remote mode needs no on-device daemon service.
                    navController.navigate(Destination.Dashboard.route) {
                        popUpTo(Destination.Welcome.route) { inclusive = true }
                    }
                })
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
                DashboardScreen(
                    vm,
                    onOpenNetworks = { navController.navigate(Destination.Networks.route) },
                    onOpenVolumes = { navController.navigate(Destination.Volumes.route) },
                )
            }
            composable(Destination.Containers.route) {
                val vm: ContainersViewModel = viewModel(factory = factory)
                ContainersScreen(
                    vm,
                    onOpen = { id -> navController.navigate("container/$id") },
                    onOpenPort = { port ->
                        val host = (container.connectionManager.connection.value as? com.dockerdroid.app.data.remote.Connection.Remote)?.host?.host ?: "127.0.0.1"
                        openUrl("http://$host:$port")
                    },
                )
            }
            composable(Destination.ContainerDetail.route) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                val vm: ContainerDetailViewModel = viewModel(factory = factory)
                ContainerDetailScreen(vm, id)
            }
            composable(Destination.Networks.route) {
                val vm: NetworksViewModel = viewModel(factory = factory)
                NetworksScreen(vm)
            }
            composable(Destination.Volumes.route) {
                val vm: VolumesViewModel = viewModel(factory = factory)
                VolumesScreen(vm)
            }
            composable(Destination.ImageBuild.route) {
                val vm: ImageBuildViewModel = viewModel(factory = factory)
                ImageBuildScreen(vm)
            }
            composable(Destination.Images.route) {
                val vm: ImagesViewModel = viewModel(factory = factory)
                ImagesScreen(vm, onBuild = { navController.navigate(Destination.ImageBuild.route) })
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
                val scope = rememberCoroutineScope()
                val ctx = navController.context
                val exporter = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json"),
                ) { uri ->
                    uri?.let {
                        scope.launch {
                            val json = vm.exportBackup()
                            runCatching {
                                ctx.contentResolver.openOutputStream(it)?.use { os -> os.write(json.toByteArray()) }
                            }
                        }
                    }
                }
                val importer = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    uri?.let {
                        scope.launch {
                            val json = ctx.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                            if (json != null) vm.importBackup(json)
                        }
                    }
                }
                SettingsScreen(
                    vm,
                    onExportStacks = { exporter.launch("dockerdroid-stacks.json") },
                    onImportStacks = { importer.launch(arrayOf("application/json", "*/*")) },
                )
            }
        }
    }
}
