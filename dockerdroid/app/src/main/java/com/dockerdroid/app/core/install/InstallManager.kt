package com.dockerdroid.app.core.install

import com.dockerdroid.app.core.shell.RootShellManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Coarse stages emitted while installing, for the progress UI. */
sealed interface InstallProgress {
    data class Step(val index: Int, val total: Int, val message: String) : InstallProgress
    data class Failed(val message: String) : InstallProgress
    data object Done : InstallProgress
}

/**
 * Performs the one-tap installation:
 *   1. lay down the directory tree under [Paths.ROOT]
 *   2. download + verify Docker / containerd / runc (delegated to the shell script
 *      so checksum verification happens with root, off the main thread)
 *   3. write daemon.json + networking + storage-driver config
 *   4. validate the install by running `dockerd --version`
 *
 * The heavy lifting lives in `assets/install-docker.sh` (mirrored at
 * `scripts/install-docker.sh`) which is pushed to the device and executed as root;
 * keeping it in shell makes the flow auditable and easy to run standalone.
 */
class InstallManager(
    private val shell: RootShellManager,
    private val readInstallScript: () -> String,
) {

    object Paths {
        const val ROOT = "/data/local/docker"
        const val BIN = "$ROOT/bin"
        const val LIB = "$ROOT/lib"
        const val DATA = "$ROOT/data"
        const val SOCKET = "$ROOT/docker.sock"
        const val DAEMON_JSON = "$ROOT/daemon.json"
        const val SCRIPT = "$ROOT/install-docker.sh"
    }

    /** Emits [InstallProgress] as the install proceeds. Cold; re-runnable. */
    fun install(runtime: CompatibilityReport.Runtime): Flow<InstallProgress> = flow {
        val total = 6
        emit(InstallProgress.Step(1, total, "Creating directories"))
        val mk = shell.execScript(
            listOf("mkdir -p ${Paths.BIN} ${Paths.LIB} ${Paths.DATA}", "chmod 700 ${Paths.ROOT}"),
        )
        if (!mk.isSuccess) {
            emit(InstallProgress.Failed("Could not create ${Paths.ROOT}: ${mk.err}"))
            return@flow
        }

        emit(InstallProgress.Step(2, total, "Staging installer"))
        // Heredoc is quoted ('DOCKERDROID_EOF') so the script is written verbatim,
        // with no shell expansion of its contents.
        shell.exec("cat > ${Paths.SCRIPT} <<'DOCKERDROID_EOF'\n${readInstallScript()}\nDOCKERDROID_EOF")
        shell.exec("chmod 700 ${Paths.SCRIPT}")

        emit(InstallProgress.Step(3, total, "Downloading & verifying binaries"))
        val download = shell.execScript(
            listOf("DOCKERDROID_RUNTIME=${runtime.name} sh ${Paths.SCRIPT} download"),
            timeoutMs = 10 * 60_000L,
        )
        if (!download.isSuccess) {
            emit(InstallProgress.Failed("Download/verification failed: ${download.err}"))
            return@flow
        }

        emit(InstallProgress.Step(4, total, "Writing daemon configuration"))
        writeDaemonJson(storageDriver = if (runtime == CompatibilityReport.Runtime.DOCKER) "overlay2" else "vfs")

        emit(InstallProgress.Step(5, total, "Configuring networking"))
        shell.exec("sh ${Paths.SCRIPT} network")

        emit(InstallProgress.Step(6, total, "Validating installation"))
        val verify = shell.exec("${Paths.BIN}/dockerd --version")
        if (verify.isSuccess) {
            emit(InstallProgress.Done)
        } else {
            emit(InstallProgress.Failed("dockerd did not run: ${verify.err}"))
        }
    }

    private suspend fun writeDaemonJson(storageDriver: String) {
        val json = """
            {
              "hosts": ["unix://${Paths.SOCKET}"],
              "data-root": "${Paths.DATA}",
              "storage-driver": "$storageDriver",
              "iptables": false,
              "bridge": "none",
              "experimental": false,
              "log-driver": "json-file",
              "log-opts": { "max-size": "10m", "max-file": "3" }
            }
        """.trimIndent()
        shell.exec("cat > ${Paths.DAEMON_JSON} <<'DOCKERDROID_EOF'\n$json\nDOCKERDROID_EOF")
    }

    suspend fun isInstalled(): Boolean =
        shell.exec("test -x ${Paths.BIN}/dockerd && echo ok").out.contains("ok")
}
