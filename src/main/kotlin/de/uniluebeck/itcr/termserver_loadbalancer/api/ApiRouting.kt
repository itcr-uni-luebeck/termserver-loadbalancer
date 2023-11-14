package de.uniluebeck.itcr.termserver_loadbalancer.api

import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.endpoints
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.loadBalancerConf
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.Endpoint
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.EndpointRole
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.validateEndpoint
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.OurMetrics
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.OurMetrics.Companion.appMicrometerRegistry
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Metrics.counter
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4b.model.OperationOutcome

fun Route.routeApi() {
    route("/endpoints") {
        endpointsRoute()
    }

    get("/load-balancer") {
        val state = loadBalancerConf.loadBalancingState
        call.respond(state)
    }
}

private fun Route.endpointsRoute() {
    get {
        appMicrometerRegistry.counter("endpoints.list").increment()
        call.respond(endpoints.getEndpoints())
    }

    route("validate") {
        post {
            val endpoint = call.receive<Endpoint>()
            logger.info("Validating endpoint: ${endpoint.uri}")
            appMicrometerRegistry.counter("endpoints.validate", "endpointId", endpoint.id).increment()
            call.respond(validateEndpoint(endpoint))
        }
    }

    post {
        val endpoint: Endpoint = when (call.request.contentType()) {
            ContentType.Application.FormUrlEncoded -> {
                val params = call.receiveParameters()
                val url = params["uri"] ?: throw ApiException(
                    "URI must be set",
                    OperationOutcome.IssueType.INVALID,
                    HttpStatusCode.BadRequest
                )
                val name = params["name"] ?: throw ApiException(
                    "Name must be set",
                    OperationOutcome.IssueType.INVALID,
                    HttpStatusCode.BadRequest
                )
                Endpoint(
                    url = url,
                    name = name
                )
            }

            else -> call.receive()
        }
        appMicrometerRegistry.counter("endpoints.create", "endpointId", endpoint.id).increment()
        if (endpoint.id?.isNotBlank() == true) {
            throw ApiException(
                "ID must not be set, it will be automatically generated",
                OperationOutcome.IssueType.INVALID,
                HttpStatusCode.BadRequest
            )
        }
        if (endpoint.name.isNullOrBlank()) {
            throw ApiException("Name must be set", OperationOutcome.IssueType.INVALID, HttpStatusCode.BadRequest)
        }
        val endpointDetails = validateEndpoint(endpoint)
        val newEndpoint = endpoints.addEndpoint(endpoint, endpointDetails)
        call.respond(newEndpoint)
    }

    route("{endpoint_id}") {
        get {
            val endpointId = call.getEndpointId()
            val endpoint = endpoints.getEndpoints(endpointId)
            appMicrometerRegistry.counter("endpoint.get", "endpointId", endpointId).increment()
            if (endpoint == null) {
                throw ApiException(
                    "Endpoint $endpointId not found",
                    OperationOutcome.IssueType.NOTFOUND,
                    HttpStatusCode.NotFound
                )
            } else {
                call.respond(endpoint)
            }
        }

        delete {
            deleteEndpoint()
        }

        endpointSubRoutes()
    }
}

private fun Route.endpointSubRoutes() {
    route("role") {

        @Serializable
        data class RoleRequest(val role: EndpointRole, val id: String? = null)

        get {
            val endpointId = call.getEndpointId()
            appMicrometerRegistry.counter("endpoint.role.get", "endpointId", endpointId).increment()
            val endpoint = endpoints.getEndpoints(endpointId)
            if (endpoint == null) {
                throw NotFoundException("Endpoint $endpointId not found")
            } else {
                val role = loadBalancerConf.loadBalancingState.endpointRoleMap[endpointId]
                if (role == null) {
                    throw IllegalStateException("Endpoint $endpointId has no role")
                } else {
                    call.respond(RoleRequest(role, id = endpointId))
                }
            }
        }

        post {
            val endpointId = call.getEndpointId()
            if (endpointId !in endpoints.getEndpoints().mapNotNull { it.id }) {
                throw NotFoundException("Endpoint $endpointId not found")
            }
            appMicrometerRegistry.counter("endpoint.role.set", "endpointId", endpointId).increment()
            val data = call.receive<RoleRequest>()
            val newConfig = loadBalancerConf.designateEndpointAs(endpointId, data.role)
            call.respond(newConfig)
        }
    }
    route("readonly") {
        @Serializable
        data class ReadonlyRequest(val readonly: Boolean, val id: String? = null)

        get {
            val endpointId = call.getEndpointId()
            val endpoint = endpoints.getEndpoints(endpointId)
            appMicrometerRegistry.counter("endpoint.readonly.get", "endpointId", endpointId).increment()
            if (endpoint == null) {
                throw NotFoundException("Endpoint $endpointId not found")
            } else {
                val readonly = loadBalancerConf.loadBalancingState.endpointReadonlyMap[endpointId]
                if (readonly == null) {
                    throw IllegalStateException("Endpoint $endpointId has no readonly setting")
                } else {
                    call.respond(ReadonlyRequest(readonly, id = endpointId))
                }
            }
        }
        post {
            val endpointId = call.getEndpointId()
            if (endpointId !in endpoints.getEndpoints().mapNotNull { it.id }) {
                throw NotFoundException("Endpoint $endpointId not found")
            }
            appMicrometerRegistry.counter("endpoint.readonly.set", "endpointId", endpointId).increment()
            val data = call.receive<ReadonlyRequest>()
            val newConfig = loadBalancerConf.readonlyEndpoint(endpointId, data.readonly)
            call.respond(newConfig)
        }
    }

    route("enabled") {
        @Serializable
        data class EnabledRequest(val enabled: Boolean, val id: String? = null)

        get {
            appMicrometerRegistry.counter("endpoint.enabled.get").increment()
            val endpointId = call.getEndpointId()
            val endpoint = endpoints.getEndpoints(endpointId)
            if (endpoint == null) {
                throw NotFoundException("Endpoint $endpointId not found")
            } else {
                val enabled = loadBalancerConf.loadBalancingState.endpointStatusMap[endpointId]
                if (enabled == null) {
                    throw IllegalStateException("Endpoint $endpointId has no readonly setting")
                } else {
                    call.respond(EnabledRequest(enabled, id = endpointId))
                }
            }
        }
        post {
            val endpointId = call.getEndpointId()
            if (endpointId !in endpoints.getEndpoints().mapNotNull { it.id }) {
                throw NotFoundException("Endpoint $endpointId not found")
            }
            val data = call.receive<EnabledRequest>()
            val newConfig = loadBalancerConf.readonlyEndpoint(endpointId, data.enabled)
            appMicrometerRegistry.counter("endpoint.enabled.set", "endpointId", endpointId).increment()
            call.respond(newConfig)
        }
    }
}


fun ApplicationCall.getEndpointId(): String {
    return this.parameters["endpoint_id"] ?: throw ApiException(
        "Endpoint ID must be set",
        OperationOutcome.IssueType.INVALID,
        HttpStatusCode.BadRequest
    )
}

private suspend fun PipelineContext<Unit, ApplicationCall>.deleteEndpoint() {
    val endpointId =
        call.parameters["endpoint_id"] ?: throw ApiException(
            "Endpoint ID must be set",
            OperationOutcome.IssueType.INVALID,
            HttpStatusCode.BadRequest
        )
    appMicrometerRegistry.counter("endpoint.delete.get", "endpointId", endpointId).increment()
    val endpoint = endpoints.getEndpoints(endpointId)
    if (endpoint == null) {
        throw ApiException(
            "Endpoint $endpointId not found",
            OperationOutcome.IssueType.NOTFOUND,
            HttpStatusCode.NotFound
        )
    } else {
        endpoints.removeEndpoint(endpointId)
        if (call.request.accept()?.contains("html") == true) {
            call.respondRedirect("/ui")
        }
        call.respond(HttpStatusCode.NoContent)
    }
}

class ApiException(message: String, val issueType: OperationOutcome.IssueType, val statusCode: HttpStatusCode) :
    Exception(message)