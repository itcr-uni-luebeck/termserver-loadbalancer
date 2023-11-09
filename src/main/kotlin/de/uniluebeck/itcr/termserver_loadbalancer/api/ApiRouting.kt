package de.uniluebeck.itcr.termserver_loadbalancer.api

import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.endpoints
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.loadBalancerConf
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import de.uniluebeck.itcr.termserver_loadbalancer.models.Endpoint
import de.uniluebeck.itcr.termserver_loadbalancer.models.validateEndpoint
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
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
        call.respond(endpoints.getEndpoints())
    }

    route("validate") {
        post {
            val endpoint = call.receive<Endpoint>()
            logger.info("Validating endpoint: ${endpoint.uri}")
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
        if (endpoint.uuid != null) {
            throw return@post
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
            val endpointId = call.parameters["endpoint_id"] ?: throw ApiException(
                "Endpoint ID must be set",
                OperationOutcome.IssueType.INVALID,
                HttpStatusCode.BadRequest
            )
            val endpoint = endpoints.getEndpoints(endpointId)
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

        post("/delete") {
            deleteEndpoint()
        }

        delete {
            deleteEndpoint()
        }
    }
}


private suspend fun PipelineContext<Unit, ApplicationCall>.deleteEndpoint() {
    val endpointId =
        call.parameters["endpoint_id"] ?: throw ApiException(
            "Endpoint ID must be set",
            OperationOutcome.IssueType.INVALID,
            HttpStatusCode.BadRequest
        )
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