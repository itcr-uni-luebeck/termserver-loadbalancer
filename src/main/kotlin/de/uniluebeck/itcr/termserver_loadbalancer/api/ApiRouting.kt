package de.uniluebeck.itcr.api

import de.uniluebeck.itcr.models.Endpoint
import de.uniluebeck.itcr.models.Endpoints
import de.uniluebeck.itcr.models.Storage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.routeApi() {
    get("/endpoints") {
        call.respond(Storage.endpoints.getEndpoints())
    }

    post("/endpoints") {
        val endpoint = call.receive<Endpoint>()
        if (endpoint.id != null) {
            call.respond(HttpStatusCode.BadRequest, "Endpoint ID must not be set")
            return@post
        }
        Storage.endpoints.addEndpoint(endpoint)
        call.respond(Endpoints().getEndpoints())
    }
}