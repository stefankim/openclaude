package com.dockerdroid.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dockerdroid.app.AppContainer
import com.dockerdroid.app.data.db.entities.ComposeProjectEntity
import com.dockerdroid.app.templates.ContainerTemplate
import com.dockerdroid.app.templates.ContainerTemplates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ComposeViewModel(private val container: AppContainer) : ViewModel() {

    val projects: StateFlow<List<ComposeProjectEntity>> =
        container.composeRepository.observeProjects()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: List<ContainerTemplate> = ContainerTemplates.all

    fun import(name: String, yaml: String) = viewModelScope.launch {
        runCatching { container.composeRepository.import(name, yaml) }
    }

    fun deployTemplate(template: ContainerTemplate) = viewModelScope.launch {
        val id = container.composeRepository.import(template.title, template.composeYaml)
        container.composeRepository.deploy(id)
    }

    fun deploy(projectId: Long) = viewModelScope.launch {
        container.composeRepository.deploy(projectId)
    }

    fun stop(projectId: Long) = viewModelScope.launch {
        container.composeRepository.stopStack(projectId)
    }

    fun delete(projectId: Long) = viewModelScope.launch {
        container.composeRepository.deleteStack(projectId)
    }
}
