package com.dockerdroid.app.templates

/**
 * One-click deployable application templates. Each is a ready-to-run Compose stack
 * so the Compose deploy path and the template path share a single code route.
 */
data class ContainerTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val composeYaml: String,
)

object ContainerTemplates {

    val all: List<ContainerTemplate> = listOf(
        ContainerTemplate(
            id = "ubuntu",
            title = "Ubuntu Shell",
            description = "A plain Ubuntu container you can exec into from the terminal.",
            category = "OS",
            composeYaml = """
                services:
                  ubuntu:
                    image: ubuntu:24.04
                    command: ["sleep", "infinity"]
                    restart: unless-stopped
            """.trimIndent(),
        ),
        ContainerTemplate(
            id = "redis",
            title = "Redis",
            description = "In-memory key/value store.",
            category = "Database",
            composeYaml = """
                services:
                  redis:
                    image: redis:7
                    ports:
                      - "6379:6379"
                    restart: unless-stopped
            """.trimIndent(),
        ),
        ContainerTemplate(
            id = "postgres",
            title = "PostgreSQL",
            description = "PostgreSQL 16 with a persistent volume.",
            category = "Database",
            composeYaml = """
                services:
                  postgres:
                    image: postgres:16
                    environment:
                      - POSTGRES_PASSWORD=dockerdroid
                    ports:
                      - "5432:5432"
                    volumes:
                      - pgdata:/var/lib/postgresql/data
                    restart: unless-stopped
                volumes:
                  pgdata:
            """.trimIndent(),
        ),
        ContainerTemplate(
            id = "node",
            title = "Node.js",
            description = "Node 20 LTS runtime for a quick app server.",
            category = "Runtime",
            composeYaml = """
                services:
                  node:
                    image: node:20-alpine
                    command: ["node", "-e", "require('http').createServer((_,r)=>r.end('DockerDroid')).listen(3000)"]
                    ports:
                      - "3000:3000"
                    restart: unless-stopped
            """.trimIndent(),
        ),
        ContainerTemplate(
            id = "python",
            title = "Python",
            description = "Python 3.12 with a simple HTTP server.",
            category = "Runtime",
            composeYaml = """
                services:
                  python:
                    image: python:3.12-alpine
                    command: ["python", "-m", "http.server", "8000"]
                    ports:
                      - "8000:8000"
                    restart: unless-stopped
            """.trimIndent(),
        ),
        ContainerTemplate(
            id = "wordpress",
            title = "WordPress",
            description = "WordPress with a dedicated MariaDB backend.",
            category = "App",
            composeYaml = """
                services:
                  db:
                    image: mariadb:11
                    environment:
                      - MARIADB_DATABASE=wordpress
                      - MARIADB_USER=wp
                      - MARIADB_PASSWORD=wp
                      - MARIADB_ROOT_PASSWORD=root
                    volumes:
                      - dbdata:/var/lib/mysql
                    restart: unless-stopped
                  wordpress:
                    image: wordpress:6
                    depends_on:
                      - db
                    environment:
                      - WORDPRESS_DB_HOST=db
                      - WORDPRESS_DB_USER=wp
                      - WORDPRESS_DB_PASSWORD=wp
                      - WORDPRESS_DB_NAME=wordpress
                    ports:
                      - "8080:80"
                    restart: unless-stopped
                volumes:
                  dbdata:
            """.trimIndent(),
        ),
        ContainerTemplate(
            id = "drupal",
            title = "Drupal (PHP + Nginx + MariaDB)",
            description = "Drupal CMS backed by MariaDB.",
            category = "App",
            composeYaml = """
                services:
                  mariadb:
                    image: mariadb:11
                    environment:
                      - MARIADB_DATABASE=drupal
                      - MARIADB_USER=drupal
                      - MARIADB_PASSWORD=drupal
                      - MARIADB_ROOT_PASSWORD=root
                    volumes:
                      - drupaldb:/var/lib/mysql
                    restart: unless-stopped
                  drupal:
                    image: drupal:10
                    depends_on:
                      - mariadb
                    ports:
                      - "8090:80"
                    restart: unless-stopped
                volumes:
                  drupaldb:
            """.trimIndent(),
        ),
        ContainerTemplate(
            id = "portainer",
            title = "Portainer CE",
            description = "Web UI to manage the local Docker engine (Phase 2).",
            category = "Tools",
            composeYaml = """
                services:
                  portainer:
                    image: portainer/portainer-ce:latest
                    ports:
                      - "9000:9000"
                    volumes:
                      - /data/local/docker/docker.sock:/var/run/docker.sock
                      - portainer:/data
                    restart: unless-stopped
                volumes:
                  portainer:
            """.trimIndent(),
        ),
    )

    fun byId(id: String): ContainerTemplate? = all.firstOrNull { it.id == id }
}
