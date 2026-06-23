package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dockerdroid.app.AppContainer

/**
 * Tiny factory that injects [AppContainer] into ViewModels without a DI framework.
 */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(InstallViewModel::class.java) -> InstallViewModel(container)
        modelClass.isAssignableFrom(RemoteViewModel::class.java) -> RemoteViewModel(container)
        modelClass.isAssignableFrom(VmViewModel::class.java) -> VmViewModel(container)
        modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container)
        modelClass.isAssignableFrom(ContainersViewModel::class.java) -> ContainersViewModel(container)
        modelClass.isAssignableFrom(ImagesViewModel::class.java) -> ImagesViewModel(container)
        modelClass.isAssignableFrom(ComposeViewModel::class.java) -> ComposeViewModel(container)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
        else -> error("Unknown ViewModel: ${modelClass.name}")
    } as T
}
