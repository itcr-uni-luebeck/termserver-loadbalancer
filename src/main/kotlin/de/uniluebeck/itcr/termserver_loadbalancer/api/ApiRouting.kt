package de.uniluebeck.itcr.termserver_loadbalancer.api

import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.endpoints
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.loadBalancerConf
import de.uniluebeck.itcr.termserver_loadbalancer.models.Endpoint
import de.uniluebeck.itcr.termserver_loadbalancer.models.Endpoints
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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

    post {
        val endpoint = call.receive<Endpoint>()
        if (endpoint.uuid != null) {
            call.respond(HttpStatusCode.BadRequest, "Endpoint ID must not be set")
            return@post
        }
        endpoints.addEndpoint(endpoint)
        call.respond(Endpoints().getEndpoints())
    }

    route("{endpoint_id}") {
        get {
            val endpointId = call.parameters["endpoint_id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Endpoint ID must be set")
            val endpoint = endpoints.getEndpoints(endpointId)
            if (endpoint == null) {
                call.respond(HttpStatusCode.NotFound, "Endpoint not found")
            } else {
                call.respond(endpoint)
            }
        }

        delete {
            val endpointId = call.parameters["endpoint_id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Endpoint ID must be set")
            val endpoint = endpoints.getEndpoints(endpointId)
            if (endpoint == null) {
                call.respond(HttpStatusCode.NotFound, "Endpoint not found")
            } else {
                endpoints.removeEndpoint(endpointId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
