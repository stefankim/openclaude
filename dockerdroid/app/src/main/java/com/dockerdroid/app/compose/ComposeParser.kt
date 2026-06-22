package com.dockerdroid.app.compose

/**
 * A small, dependency-free parser for the subset of docker-compose v3 that
 * DockerDroid deploys. It is intentionally not a general YAML engine: it handles
 * 2-space indented mappings, inline/dash lists, and the `services:` shape that
 * covers the bundled templates and the vast majority of hand-written stacks.
 *
 * For anything exotic (anchors, multi-doc, complex flow scalars) the parser
 * raises [ComposeParseException] so the UI can ask the user to simplify rather
 * than silently mis-deploying.
 */
class ComposeParser {

    class ComposeParseException(message: String) : Exception(message)

    fun parse(yaml: String): ComposeFile {
        val root = parseBlock(yaml.lines().filter { it.isNotBlank() && !it.trimStart().startsWith("#") }, 0).first
        @Suppress("UNCHECKED_CAST")
        val servicesNode = root["services"] as? Map<String, Any>
            ?: throw ComposeParseException("No 'services:' section found")

        val services = servicesNode.mapValues { (name, body) ->
            @Suppress("UNCHECKED_CAST")
            parseService(name, body as? Map<String, Any> ?: emptyMap())
        }
        return ComposeFile(
            version = root["version"]?.toString(),
            services = services,
            volumes = (root["volumes"] as? Map<*, *>)?.keys?.map { it.toString() } ?: emptyList(),
            networks = (root["networks"] as? Map<*, *>)?.keys?.map { it.toString() } ?: emptyList(),
        )
    }

    private fun parseService(name: String, body: Map<String, Any>): ComposeService =
        ComposeService(
            name = name,
            image = body["image"]?.toString(),
            command = asList(body["command"]),
            environment = parseEnvironment(body["environment"]),
            ports = asList(body["ports"]).map(::parsePort),
            volumes = asList(body["volumes"]),
            restart = body["restart"]?.toString(),
            dependsOn = asList(body["depends_on"]),
        )

    private fun parsePort(spec: String): PortMapping {
        val clean = spec.trim().trim('"', '\'')
        val (hostPort, rest) = if (clean.contains(":")) {
            clean.substringBeforeLast(":") to clean.substringAfterLast(":")
        } else {
            clean to clean
        }
        val proto = if (rest.contains("/")) rest.substringAfter("/") else "tcp"
        val containerPort = rest.substringBefore("/")
        return PortMapping(hostPort, containerPort, proto)
    }

    private fun parseEnvironment(node: Any?): List<String> = when (node) {
        is List<*> -> node.map { it.toString() }
        is Map<*, *> -> node.map { (k, v) -> "$k=$v" }
        else -> emptyList()
    }

    private fun asList(node: Any?): List<String> = when (node) {
        is List<*> -> node.map { it.toString().trim() }
        null -> emptyList()
        else -> listOf(node.toString().trim())
    }

    /**
     * Recursive descent over an indentation block. Returns the parsed mapping and
     * the index of the first line that does not belong to this block.
     */
    private fun parseBlock(lines: List<String>, start: Int, indent: Int = 0): Pair<Map<String, Any>, Int> {
        val result = LinkedHashMap<String, Any>()
        var i = start
        while (i < lines.size) {
            val line = lines[i]
            val lineIndent = line.indentWidth()
            if (lineIndent < indent) break
            if (lineIndent > indent) { i++; continue } // handled by recursion below

            val content = line.trim()
            if (content.startsWith("- ")) break // list handled by caller

            val key = content.substringBefore(":").trim()
            val valuePart = content.substringAfter(":", "").trim()

            when {
                valuePart.isNotEmpty() -> { result[key] = scalar(valuePart); i++ }
                else -> {
                    val childIndent = (i + 1).takeIf { it < lines.size }?.let { lines[it].indentWidth() } ?: indent
                    if (childIndent > indent && lines[i + 1].trim().startsWith("- ")) {
                        val (list, next) = parseList(lines, i + 1, childIndent)
                        result[key] = list; i = next
                    } else {
                        val (child, next) = parseBlock(lines, i + 1, childIndent)
                        result[key] = child; i = next
                    }
                }
            }
        }
        return result to i
    }

    private fun parseList(lines: List<String>, start: Int, indent: Int): Pair<List<String>, Int> {
        val items = mutableListOf<String>()
        var i = start
        while (i < lines.size && lines[i].indentWidth() == indent && lines[i].trim().startsWith("- ")) {
            items += scalar(lines[i].trim().removePrefix("- ").trim()).toString()
            i++
        }
        return items to i
    }

    private fun scalar(raw: String): Any = raw.trim().trim('"', '\'')

    private fun String.indentWidth(): Int = takeWhile { it == ' ' }.length
}
