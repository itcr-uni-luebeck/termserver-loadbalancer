package de.uniluebeck.itcr.termserver_loadbalancer.models

import de.uniluebeck.itcr.termserver_loadbalancer.JsonBackedStorage
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.loadBalancerConf
import de.uniluebeck.itcr.termserver_loadbalancer.client.FhirAwareClient
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4b.model.CapabilityStatement
import java.net.URI
import java.util.*

@Serializable
data class Endpoint(
    val uuid: String? = null,
    private val url: String,
    val name: String? = null,
    val endpointDetails: EndpointDetails? = null
) {
    val uri: URI get() = URI(url.removeSuffix("/").plus("/"))

    fun resolveUri(path: String): URI = uri.resolve(path.removePrefix("/"))
}

class Endpoints : JsonBackedStorage<Endpoint>() {
    private val endpointList by lazy {
        mutableListOf<Endpoint>().also { endpoints ->
            val readEndpoints: List<Endpoint> = when (fileLocation.exists()) {
                true -> readFromStorage()
                else -> {
                    logger.info("No endpoints.json found, creating new empty file")
                    listOf()
                }
            }
            endpoints.addAll(readEndpoints)
        }
    }

    fun getEndpoints(): List<Endpoint> = endpointList.toList()

    fun addEndpoint(endpoint: Endpoint, endpointDetails: EndpointDetails): Endpoint = when {
        endpointList.any { it.uri == endpoint.uri } -> {
            throw EndpointSettingsError("Endpoint URI already exists")
        }
        endpointList.any { it.name == endpoint.name } -> {
            throw EndpointSettingsError("Endpoint name already exists")
        }
        else -> {
            val uuid = UUID.randomUUID().toString()
            val newEndpoint = endpoint.copy(uuid = uuid, endpointDetails = endpointDetails)
            endpointList.add(newEndpoint)
            writeEndpoints()
            loadBalancerConf.addEndpoint(uuid)
            newEndpoint
        }
    }

    private fun writeEndpoints() = writeToStorage(endpointList.toList())

    fun getEndpoints(endpointId: String): Endpoint? = endpointList.find { it.uuid.toString() == endpointId }
    fun removeEndpoint(endpointId: String) {
        endpointList.removeIf { it.uuid.toString() == endpointId }
        loadBalancerConf.removeEndpoint(endpointId)
        writeEndpoints()
    }
}

suspend fun validateEndpoint(endpoint: Endpoint): EndpointDetails {
    val capabilityStatement = FhirAwareClient.getDomainResource<CapabilityStatement>(endpoint, "metadata")
    val fhirVersion = capabilityStatement.fhirVersionElement.asStringValue()
    return EndpointDetails(
        fhirVersion = "FHIR $fhirVersion",
        softwareName = capabilityStatement.software.name,
        softwareVersion = capabilityStatement.software.version
    )
}

@Serializable
data class EndpointDetails(
    val fhirVersion: String,
    val softwareName: String,
    val softwareVersion: String?,
)

class EndpointSettingsError(message: String) : Exception(message)
