package com.dockerdroid.app.data.repository

import com.dockerdroid.app.compose.ComposeParser
import com.dockerdroid.app.compose.ComposeService
import com.dockerdroid.app.data.api.DockerApiClient
import com.dockerdroid.app.data.api.models.CreateContainerRequest
import com.dockerdroid.app.data.api.models.HostConfig
import com.dockerdroid.app.data.api.models.PortBinding
import com.dockerdroid.app.data.api.models.RestartPolicy
import com.dockerdroid.app.data.db.dao.ComposeProjectDao
import com.dockerdroid.app.data.db.dao.DeployedContainerDao
import com.dockerdroid.app.data.db.entities.ComposeProjectEntity
import com.dockerdroid.app.data.db.entities.DeployedContainerEntity
import com.dockerdroid.app.data.db.entities.StackStatus
import kotlinx.coroutines.flow.Flow

/**
 * Owns the lifecycle of Compose stacks: import → deploy → stop → delete.
 *
 * Deployment is a deliberately simple translation of each service into a
 * `containers/create` call (image pull + create + start), wiring `depends_on`
 * ordering and the project's containers into a single record set so the whole
 * stack can be torn down together.
 */
class ComposeRepository(
    private val apiProvider: () -> DockerApiClient,
    private val parser: ComposeParser,
    private val projectDao: ComposeProjectDao,
    private val containerDao: DeployedContainerDao,
) {
    private val api: DockerApiClient get() = apiProvider()

    fun observeProjects(): Flow<List<ComposeProjectEntity>> = projectDao.observeAll()

    suspend fun import(name: String, yaml: String): Long {
        parser.parse(yaml) // validate eagerly; throws if malformed
        return projectDao.upsert(ComposeProjectEntity(name = name, yaml = yaml))
    }

    /** Deploy every service in a stack. Returns true if all containers started. */
    suspend fun deploy(projectId: Long): Boolean {
        val project = projectDao.byId(projectId) ?: return false
        projectDao.setStatus(projectId, StackStatus.DEPLOYING)
        val parsed = runCatching { parser.parse(project.yaml) }.getOrElse {
            projectDao.setStatus(projectId, StackStatus.FAILED)
            return false
        }

        val records = mutableListOf<DeployedContainerEntity>()
        val ordered = topoSort(parsed.services)
        for (service in ordered) {
            val image = service.image ?: continue
            api.pullImage(image).collect { /* progress consumed by UI elsewhere */ }
            val id = api.createContainer(
                name = "${project.name}_${service.name}",
                body = service.toCreateRequest(),
            )
            if (!api.startContainer(id)) {
                projectDao.setStatus(projectId, StackStatus.FAILED)
                return false
            }
            records += DeployedContainerEntity(id, projectId, service.name, image)
        }
        containerDao.insertAll(records)
        projectDao.setStatus(projectId, StackStatus.RUNNING)
        return true
    }

    suspend fun stopStack(projectId: Long) {
        containerDao.forProject(projectId).forEach { api.stopContainer(it.containerId) }
        projectDao.setStatus(projectId, StackStatus.STOPPED)
    }

    suspend fun deleteStack(projectId: Long) {
        containerDao.forProject(projectId).forEach { api.removeContainer(it.containerId, force = true) }
        containerDao.deleteForProject(projectId)
        projectDao.delete(projectId)
    }

    private fun ComposeService.toCreateRequest(): CreateContainerRequest {
        val exposed = ports.associate { it.containerKey to emptyMap<String, Any>() }
        val bindings = ports.associate {
            it.containerKey to listOf(PortBinding(it.host))
        }
        return CreateContainerRequest(
            image = image!!,
            cmd = command.ifEmpty { null },
            env = environment.ifEmpty { null },
            exposedPorts = exposed.ifEmpty { null },
            hostConfig = HostConfig(
                portBindings = bindings.ifEmpty { null },
                binds = volumes.ifEmpty { null },
                restartPolicy = restart?.let { RestartPolicy(it) },
            ),
        )
    }

    /** Order services so dependencies start first (best-effort; cycles fall through). */
    private fun topoSort(services: Map<String, ComposeService>): List<ComposeService> {
        val visited = LinkedHashSet<String>()
        val out = mutableListOf<ComposeService>()
        fun visit(name: String) {
            if (!visited.add(name)) return
            val svc = services[name] ?: return
            svc.dependsOn.forEach(::visit)
            out += svc
        }
        services.keys.forEach(::visit)
        return out
    }
}
