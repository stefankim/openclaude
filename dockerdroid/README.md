# DockerDroid

> A "Docker Desktop for Android" experience. Install, configure, manage, and use a
> full Docker Engine on a **rooted** Android device through a graphical interface —
> no manual Termux commands required.

DockerDroid is a native Android app (Kotlin · Jetpack Compose · Material 3 · MVVM)
that drives the Docker Engine entirely over root, exposing one-tap install, daemon
control, container/image/volume/network management, Compose deploys, an embedded
terminal, live monitoring, and boot persistence.

> ⚠️ **The hard part isn't the app — it's the kernel.** Android kernels vary wildly
> in their cgroup/namespace/overlayfs/netfilter support. DockerDroid ships a
> [`CompatibilityChecker`](app/src/main/java/com/dockerdroid/app/core/install/CompatibilityChecker.kt)
> that probes the kernel up front and **falls back to a rootless Podman/containerd
> runtime** when Docker-specific features are missing, rather than letting `dockerd`
> fail cryptically. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Status

This repository contains a complete, modular **architecture and reference
implementation**. The Kotlin/Compose layers, daemon supervision, Docker API client,
Compose parser, Room schema, tests, and CI are all in place. Binary download hashes
are pinned per-release by CI (see `BinarySource.kt`), and the Gradle wrapper jar is
generated on first build (`gradle wrapper`).

## Target environment

| | |
|---|---|
| OS | Android 12+ (minSdk 31, target 35) |
| Root | Magisk (libsu / MagiskSU) |
| Arch | arm64-v8a |
| Daemon | Docker Engine static build, fallback to Podman/containerd |

## Architecture at a glance

```
┌──────────────────────────── Layer 1: UI (Compose) ────────────────────────────┐
│ Welcome · Install · Dashboard · Containers · Images · Compose · Terminal · …   │
│ MVVM ViewModels  ◄── StateFlow ──  Repositories                                 │
└───────────────────────────────────────────────────────────────────────────────┘
                │                                  │
┌─────────────── Layer 2: Service ──────────────┐ │  DockerApiClient (OkHttp over
│ DockerForegroundService                       │ │  the unix socket)
│ DockerServiceManager  (start/stop/supervise)  │ │
└───────────────────────────────────────────────┘ │
                │                                  │
┌──────────────────────── Layer 3: Shell Execution ────────────────────────────┐
│ RootShellManager (libsu) — exec / stream / queue, MOUNT_MASTER namespace       │
└───────────────────────────────────────────────────────────────────────────────┘
                │
        dockerd / containerd / runc  under  /data/local/docker
```

Full breakdown: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) ·
package map: [docs/PACKAGE_STRUCTURE.md](docs/PACKAGE_STRUCTURE.md) ·
DB schema: [docs/DATABASE.md](docs/DATABASE.md) ·
wireframes: [docs/WIREFRAMES.md](docs/WIREFRAMES.md).

## Features

- **One-tap install** — directory layout, binary download + SHA-256 verification,
  `daemon.json`, networking, storage-driver selection, validation, progress UI.
- **Daemon control** — start/stop/restart with crash auto-restart, state via `Flow`.
- **Containers** — create, start, stop, restart, delete, inspect, logs, stats.
- **Images** — pull (streamed progress), list, remove.
- **Volumes & networks** — list, create, delete.
- **Compose** — import YAML from storage, deploy as containers, stop/delete stacks.
- **Templates** — one-click Ubuntu, Redis, PostgreSQL, Node, Python, WordPress,
  Drupal, Portainer.
- **Embedded terminal** — streamed root shell with Docker CLI access.
- **Monitoring** — per-container CPU / RAM / network, refreshed live.
- **Remote hosts (SSH)** — manage a Docker Engine on another machine (e.g. Docker
  Desktop on a Mac) with **no phone root**; works on locked devices. See
  [docs/REMOTE.md](docs/REMOTE.md).
- **On-device VM (no root)** — boot a real Docker Engine in a bundled QEMU Linux VM,
  so unrootable devices can run containers locally (software-emulated, slow). Engine
  + wiring are in-app; the native QEMU payload is built separately. See
  [docs/VM.md](docs/VM.md).
- **Auto start on boot** — opt-in `BOOT_COMPLETED` receiver.
- **Security** — root prompted only when needed, no stored credentials, downloaded
  binaries SHA-256 verified, app data sandboxed.

## Build

```bash
cd dockerdroid
gradle wrapper --gradle-version 8.10   # one-time: generates the wrapper jar
./gradlew assembleDebug                 # debug APK
./gradlew testDebugUnitTest             # unit tests
./gradlew assembleRelease               # release APK (configure signing)
```

Detailed instructions, signing, and install: [docs/BUILD.md](docs/BUILD.md).

## Roadmap (Phase 2)

k3s/Kubernetes · Portainer integration · local registry · image builder · container
snapshots · backup/restore · remote Docker over SSH · multi-device clusters.

## License

See the repository root `LICENSE`.
