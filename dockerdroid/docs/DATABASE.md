# Database Schema

DockerDroid uses **Room** (SQLite) for durable state that must outlive the daemon,
and **DataStore** for user preferences. The Docker engine itself remains the source
of truth for live container/image state — the database only stores app-owned data.

## Room — `dockerdroid.db` (version 1)

### `compose_projects`
Imported Compose stacks. The raw YAML is the source of truth; deployment is derived.

| column     | type   | notes                                            |
|------------|--------|--------------------------------------------------|
| id         | INTEGER| PK, autogenerate                                 |
| name       | TEXT   | display name                                     |
| yaml       | TEXT   | raw docker-compose content                       |
| status     | TEXT   | `IMPORTED`/`DEPLOYING`/`RUNNING`/`STOPPED`/`FAILED` |
| createdAt  | INTEGER| epoch millis                                     |
| updatedAt  | INTEGER| epoch millis                                     |

### `deployed_containers`
Maps a stack to the containers it created, so a stack can be torn down as a unit.

| column      | type   | notes                                              |
|-------------|--------|----------------------------------------------------|
| containerId | TEXT   | PK (Docker container id)                            |
| projectId   | INTEGER| FK → compose_projects.id, **ON DELETE CASCADE**     |
| serviceName | TEXT   | compose service key                                |
| image       | TEXT   | image reference                                    |

Index: `projectId`.

### `event_log`
App-level audit trail surfaced on the Logs screen (separate from container logs).

| column    | type   | notes                |
|-----------|--------|----------------------|
| id        | INTEGER| PK, autogenerate     |
| timestamp | INTEGER| epoch millis, indexed|
| level     | TEXT   | INFO/WARN/ERROR      |
| tag       | TEXT   | subsystem            |
| message   | TEXT   | free text            |

## DataStore — `settings`

| key            | type    | default | meaning                              |
|----------------|---------|---------|--------------------------------------|
| start_on_boot  | Boolean | false   | start the daemon after BOOT_COMPLETED|
| auto_restart   | Boolean | true    | supervise + relaunch dockerd on crash|

## Migrations

Schemas are exported (`room.schemaLocation`) for review. v1 ships with
`fallbackToDestructiveMigration` during pre-release; pinned migrations are added
before the first stable tag.
