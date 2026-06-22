# Package Structure

```
com.dockerdroid.app
├── DockerDroidApplication.kt      Application; owns AppContainer
├── AppContainer.kt                Manual DI graph (lazy singletons)
├── MainActivity.kt                Single activity; Compose NavHost + scaffold
│
├── core
│   ├── shell
│   │   ├── RootShellManager.kt    libsu-backed root execution (exec/stream/queue)
│   │   └── CommandResult.kt       CommandResult + ShellLine stream model
│   ├── service
│   │   ├── DaemonState.kt         Stopped/Starting/Running/Error
│   │   ├── DockerServiceManager.kt  dockerd lifecycle + supervisor
│   │   └── DockerForegroundService.kt  specialUse FGS + notification
│   └── install
│       ├── CompatibilityChecker.kt  Kernel/userspace probes
│       ├── CompatibilityReport.kt   Report + Runtime{DOCKER,PODMAN,UNSUPPORTED}
│       ├── InstallManager.kt        One-tap install flow + Paths
│       └── BinarySource.kt          Pinned downloads + SHA-256
│
├── data
│   ├── api
│   │   ├── DockerApiClient.kt     Engine REST client (images/containers/…)
│   │   ├── UnixSocketFactory.kt   OkHttp ↔ unix domain socket
│   │   └── models/ApiModels.kt    Moshi DTOs
│   ├── db
│   │   ├── AppDatabase.kt         Room database + converters
│   │   ├── entities/Entities.kt   ComposeProject, DeployedContainer, EventLog
│   │   └── dao/Daos.kt            DAOs
│   └── repository
│       ├── ComposeRepository.kt   Import/deploy/stop/delete stacks
│       └── SettingsRepository.kt  DataStore-backed settings
│
├── compose
│   ├── ComposeParser.kt           Dependency-free compose subset parser
│   └── ComposeModels.kt           ComposeFile/Service/PortMapping
│
├── templates
│   └── ContainerTemplates.kt      One-click app templates
│
├── receiver
│   └── BootReceiver.kt            BOOT_COMPLETED → start service (opt-in)
│
└── ui
    ├── theme/Theme.kt
    ├── navigation/Destinations.kt
    ├── viewmodel/                 InstallVM, DashboardVM, ContainersVM, ImagesVM,
    │                              ComposeVM, SettingsVM, ViewModelFactory
    └── screens/                   Onboarding, Dashboard, Containers, Images,
                                   Compose, Terminal, Settings

assets/install-docker.sh           Root installer (mirrored at scripts/)
```

## Test sources

```
app/src/test/java/com/dockerdroid/app
├── ComposeParserTest.kt           Parser unit tests
└── CompatibilityCheckerTest.kt    Runtime-selection tests (mocked shell)

app/src/androidTest/java/com/dockerdroid/app
└── WelcomeScreenTest.kt           Compose UI test
```
