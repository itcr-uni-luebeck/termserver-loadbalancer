package de.uniluebeck.itcr.models

import de.uniluebeck.itcr.logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI

@Serializable
data class Endpoint(val id: Int? = null, private val url: String, val name: String) {
    val uri: URI get() = URI(url.removeSuffix("/"))
}

class Endpoints {

    private val json = Json { prettyPrint = true }
    companion object {
        private val fileLocation = File("src/main/resources/endpoints.json")
    }
    private val endpointList by lazy {
        mutableListOf<Endpoint>().also { endpoints ->
            val readEndpoints : List<Endpoint> = when (fileLocation.exists()) {
                true -> {
                    val jsonString = fileLocation.readText()
                    Json.decodeFromString(jsonString)
                }
                else -> {
                    logger.info("No endpoints.json found, creating new empty file")
                    fileLocation.createNewFile()
                    listOf()
                }
            }
            endpoints.addAll(readEndpoints)
        }
    }

    fun getEndpoints() = endpointList.toList()

    fun addEndpoint(endpoint: Endpoint) {
        val nextId = endpointList.maxOfOrNull { it.id ?: 0 } ?: 0
        when {
            endpointList.any { it.uri == endpoint.uri } -> {
                throw IllegalArgumentException("Endpoint already exists")
            }
            endpointList.any { it.name == endpoint.name } -> {
                throw IllegalArgumentException("Endpoint name already exists")
            }
            else -> {
                endpointList.add(endpoint.copy(id = nextId + 1))
                val jsonString = json.encodeToString(endpointList.toList())

                fileLocation.writeText(jsonString)
            }
        }
    }
}