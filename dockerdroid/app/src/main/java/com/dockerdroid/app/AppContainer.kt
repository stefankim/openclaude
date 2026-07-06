package com.dockerdroid.app

import android.content.Context
import com.dockerdroid.app.compose.ComposeParser
import com.dockerdroid.app.core.install.CompatibilityChecker
import com.dockerdroid.app.core.NotificationHelper
import com.dockerdroid.app.core.install.InstallManager
import com.dockerdroid.app.core.service.DockerServiceManager
import com.dockerdroid.app.core.shell.RootShellManager
import com.dockerdroid.app.data.api.DockerApiClient
import com.dockerdroid.app.data.api.DockerEventsMonitor
import com.dockerdroid.app.data.db.AppDatabase
import com.dockerdroid.app.data.remote.ConnectionManager
import com.dockerdroid.app.data.repository.ComposeRepository
import com.dockerdroid.app.data.repository.SettingsRepository
import com.dockerdroid.app.vm.QemuVmManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Hand-rolled dependency container. The app is small enough that a manual graph is
 * clearer than a DI framework; everything is a lazy singleton scoped to the process.
 */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob())

    val shell: RootShellManager by lazy { RootShellManager() }
    val database: AppDatabase by lazy { AppDatabase.get(context) }
    val composeParser: ComposeParser by lazy { ComposeParser() }

    /** Owns whether we talk to the on-device daemon or a remote SSH host. */
    val connectionManager: ConnectionManager by lazy { ConnectionManager(context) }

    /** mDNS discovery of SSH hosts on the LAN. */
    val hostDiscovery: com.dockerdroid.app.data.remote.HostDiscovery by lazy {
        com.dockerdroid.app.data.remote.HostDiscovery(context)
    }

    /** The active Docker client; tracks the current connection (local or remote). */
    val api: DockerApiClient get() = connectionManager.client

    val compatibilityChecker: CompatibilityChecker by lazy { CompatibilityChecker(shell) }

    val installManager: InstallManager by lazy {
        InstallManager(shell, readInstallScript = {
            context.assets.open("install-docker.sh").bufferedReader().use { it.readText() }
        })
    }

    val serviceManager: DockerServiceManager by lazy { DockerServiceManager(shell, appScope) }

    /** On-device QEMU VM that runs a real Docker daemon with no root. */
    val vmManager: QemuVmManager by lazy { QemuVmManager(context, appScope) }

    val notifications: NotificationHelper by lazy { NotificationHelper(context) }

    /** Broadcasts container lifecycle events + posts crash notifications. */
    val eventsMonitor: DockerEventsMonitor by lazy {
        DockerEventsMonitor(connectionManager, notifications, appScope)
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }

    val composeRepository: ComposeRepository by lazy {
        ComposeRepository(
            // Provider, not a captured instance, so deploys follow the active connection.
            apiProvider = { connectionManager.client },
            parser = composeParser,
            projectDao = database.composeProjectDao(),
            containerDao = database.deployedContainerDao(),
        )
    }
}
