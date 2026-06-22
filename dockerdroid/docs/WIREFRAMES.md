# UI Wireframes

ASCII wireframes for the primary screens. The real UI is Jetpack Compose + Material 3
with dynamic color; these communicate layout and flow.

## Onboarding flow

```
 Welcome              Install (root + compat + install)        Dashboard
┌──────────────┐     ┌───────────────────────────────┐     ┌──────────────┐
│              │     │ Environment check             │     │ Docker Engine│
│  DockerDroid │     │ ✓ Root access                 │     │ ● Running    │
│              │ ──▶ │ ✓ CPU architecture  aarch64   │ ──▶ │   pid 2841   │
│  Run Docker  │     │ ✓ Control groups    cgroup v2 │     │  [ Stop ]    │
│  on Android  │     │ ⚠ Storage driver    vfs       │     ├──────┬───────┤
│              │     │ ✓ Namespaces                  │     │ 3/5  │  12   │
│ [Get started]│     │ Recommended runtime: DOCKER   │     │ cont │ images│
└──────────────┘     │ [ Install with one tap ]      │     └──────┴───────┘
                     │ ▓▓▓▓▓▓░░░ 4/6 Writing config  │
                     └───────────────────────────────┘
```

## Containers

```
┌─────────────────────────────────┐
│ Containers                       │
│ ┌─────────────────────────────┐ │
│ │ web                         │ │
│ │ nginx:1.27                  │ │
│ │ Up 3 minutes                │ │
│ │ [Stop] [Restart] [Delete]   │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ db   postgres:16  Exited    │ │
│ │ [Start] [Restart] [Delete]  │ │
│ └─────────────────────────────┘ │
├─────────────────────────────────┤
│ 🅓  📦  🖼  🧩  ▶_  ⚙           │  ← bottom nav
└─────────────────────────────────┘
```

## Images

```
┌─────────────────────────────────┐
│ [ nginx:1.27        ] [ Pull ]   │
│ pulling: Extracting 24/31        │
│ ── nginx:1.27        187 MB ──── │
│ ── postgres:16       438 MB ──── │
└─────────────────────────────────┘
```

## Compose

```
┌── Templates ──┬── My stacks ──┐
│ Ubuntu Shell        [Deploy]  │
│ PostgreSQL          [Deploy]  │
│ WordPress           [Deploy]  │
│ Drupal              [Deploy]  │
└───────────────────────────────┘
   My stacks tab → [Import docker-compose.yml]
                   wordpress  RUNNING [Deploy][Stop][Delete]
```

## Terminal

```
┌─────────────────────────────────┐
│ DockerDroid shell — root         │
│ $ docker ps                      │
│ CONTAINER ID  IMAGE   STATUS     │
│ 9f3a...       nginx   Up 3m      │
│ $ _                              │
├─────────────────────────────────┤
│ [ command…              ] [Run]  │
└─────────────────────────────────┘
```

## Settings

```
┌─────────────────────────────────┐
│ Start Docker on boot      [ ●  ] │
│ Auto-restart on crash     [ ●  ] │
│ ───────────────────────────────  │
│ Docker Engine 27.3.1 · v0.1.0    │
└─────────────────────────────────┘
```
