package com.dockerdroid.app

import com.dockerdroid.app.compose.ComposeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeParserTest {

    private val parser = ComposeParser()

    @Test
    fun `parses single service with image and ports`() {
        val yaml = """
            services:
              web:
                image: nginx:1.27
                ports:
                  - "8080:80"
                restart: unless-stopped
        """.trimIndent()

        val file = parser.parse(yaml)

        assertEquals(1, file.services.size)
        val web = file.services.getValue("web")
        assertEquals("nginx:1.27", web.image)
        assertEquals("unless-stopped", web.restart)
        assertEquals(1, web.ports.size)
        assertEquals("8080", web.ports[0].host)
        assertEquals("80", web.ports[0].container)
        assertEquals("tcp", web.ports[0].protocol)
    }

    @Test
    fun `parses environment list and depends_on`() {
        val yaml = """
            services:
              db:
                image: postgres:16
                environment:
                  - POSTGRES_PASSWORD=secret
              app:
                image: myapp:latest
                depends_on:
                  - db
        """.trimIndent()

        val file = parser.parse(yaml)

        assertEquals(listOf("POSTGRES_PASSWORD=secret"), file.services.getValue("db").environment)
        assertEquals(listOf("db"), file.services.getValue("app").dependsOn)
    }

    @Test
    fun `parses udp port protocol`() {
        val yaml = """
            services:
              dns:
                image: coredns:latest
                ports:
                  - "53:53/udp"
        """.trimIndent()

        val port = parser.parse(yaml).services.getValue("dns").ports.single()
        assertEquals("udp", port.protocol)
        assertEquals("53/udp", port.containerKey)
    }

    @Test
    fun `parses top level volumes`() {
        val yaml = """
            services:
              db:
                image: postgres:16
                volumes:
                  - pgdata:/var/lib/postgresql/data
            volumes:
              pgdata:
        """.trimIndent()

        val file = parser.parse(yaml)
        assertTrue(file.volumes.contains("pgdata"))
        assertEquals(listOf("pgdata:/var/lib/postgresql/data"), file.services.getValue("db").volumes)
    }

    @Test
    fun `throws when services section missing`() {
        assertThrows(ComposeParser.ComposeParseException::class.java) {
            parser.parse("version: \"3\"\nnetworks:\n  default:\n")
        }
    }
}
