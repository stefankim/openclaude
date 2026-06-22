# DockerDroid Architecture

DockerDroid is a three-layer MVVM application. Each layer has one responsibility and
talks to the layer below through a narrow interface, which keeps the kernel/root
complexity isolated from the UI.

## Layer 1 — Android UI (Jetpack Compose)

- **Screens** (`ui/screens`): Welcome, Install (root detection + compatibility +
  one-tap install), Dashboard, Containers, Images, Compose, Terminal, Settings.
  Networks/Volumes/Logs reuse the same list patterns.
- **Navigation** (`ui/navigation/Destinations.kt`): a single `NavHost` with a
  bottom navigation bar for the post-install destinations.
- **ViewModels** (`ui/viewmodel`): expose immutable `StateFlow<UiState>` and call
  into repositories / managers. No Android framework types leak into them beyond
  `ViewModel`/`viewModelScope`.
- **Theme** (`ui/theme`): Material 3 with dynamic color (Android 12+).

## Layer 2 — Docker Service Manager

- **`DockerServiceManager`** owns the `dockerd` lifecycle: launches it detached via
  `setsid`, tracks the pid file, waits for the API socket, supervises liveness on a
  5s loop, and auto-restarts on crash. State is published as
  `StateFlow<DaemonState>` (`Stopped`/`Starting`/`Running(pid)`/`Error`).
- **`DockerForegroundService`** is a `specialUse` foreground service that keeps the
  daemon alive while backgrounded and mirrors `DaemonState` into its notification.
- **`DockerApiClient`** (Layer 2.5) is the HTTP client for the Engine REST API,
  dialed over the unix socket by `UnixSocketFactory`.

## Layer 3 — Shell Execution Engine

- **`RootShellManager`** wraps [libsu](https://github.com/topjohnwu/libsu). It owns a
  long-lived root shell (`FLAG_MOUNT_MASTER` so dockerd's mounts work), serializes
  commands through a `Mutex`, and offers:
  - `exec` / `execScript` — buffered `CommandResult` with timeouts.
  - `stream` — a cold `Flow<ShellLine>` for logs / terminal / `dockerd` boot output.

## Cross-cutting

- **Installation** (`core/install`): `CompatibilityChecker` → `CompatibilityReport`
  → `InstallManager`, which stages and runs `assets/install-docker.sh` as root.
- **Persistence** (`data/db`): Room for Compose projects, deployed-container records,
  and the app event log. DataStore for user settings.
- **Compose** (`compose`): a dependency-free `ComposeParser` plus `ComposeRepository`
  that translates services into `containers/create` calls with `depends_on` ordering.
- **DI**: a hand-rolled `AppContainer` of lazy singletons; `ViewModelFactory` injects
  it into ViewModels.

## Data flow example — pulling an image

```
ImagesScreen → ImagesViewModel.pull(ref)
  → DockerApiClient.pullImage(ref)  [streamed Flow<String>]
      → OkHttp POST /images/create  over  UnixSocketFactory → /data/local/docker/docker.sock
          → dockerd (Layer 2, supervised by DockerServiceManager)
  ← progress lines → UiState.pullStatus → recomposition
```

## Why a Podman fallback?

`dockerd` requires overlayfs + iptables + delegated cgroups. Many stock Android
kernels compile one or more out. The compatibility checker downgrades the
*recommended runtime* to Podman (rootless, daemonless, vfs storage, host networking)
when overlayfs **and** netfilter are both unavailable, and marks the device
`UNSUPPORTED` when namespaces are missing entirely. This is the single most important
reliability decision in the app — see [COMPATIBILITY.md](COMPATIBILITY.md).
