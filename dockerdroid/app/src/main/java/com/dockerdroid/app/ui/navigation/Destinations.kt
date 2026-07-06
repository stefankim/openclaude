package com.dockerdroid.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level navigation graph for DockerDroid. */
enum class Destination(val route: String, val label: String, val icon: ImageVector, val inBottomBar: Boolean = true) {
    Welcome("welcome", "Welcome", Icons.Filled.Dashboard, inBottomBar = false),
    RootCheck("root_check", "Root", Icons.Filled.Dashboard, inBottomBar = false),
    Install("install", "Install", Icons.Filled.Dashboard, inBottomBar = false),
    RemoteConnect("remote_connect", "Remote", Icons.Filled.Dashboard, inBottomBar = false),
    LocalVm("local_vm", "VM", Icons.Filled.Dashboard, inBottomBar = false),
    ContainerDetail("container/{id}", "Container", Icons.Filled.ViewInAr, inBottomBar = false),
    Networks("networks", "Networks", Icons.Filled.Hub, inBottomBar = false),
    Volumes("volumes", "Volumes", Icons.Filled.Storage, inBottomBar = false),
    ImageBuild("image_build", "Build", Icons.Filled.Image, inBottomBar = false),

    Dashboard("dashboard", "Dashboard", Icons.Filled.Dashboard),
    Containers("containers", "Containers", Icons.Filled.ViewInAr),
    Images("images", "Images", Icons.Filled.Image),
    Compose("compose", "Compose", Icons.Filled.Layers),
    Terminal("terminal", "Terminal", Icons.Filled.Terminal),
    Settings("settings", "Settings", Icons.Filled.Settings);

    companion object {
        val bottomBar: List<Destination> get() = entries.filter { it.inBottomBar }
    }
}
