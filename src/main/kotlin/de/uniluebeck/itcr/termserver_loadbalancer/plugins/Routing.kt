package de.uniluebeck.itcr.termserver_loadbalancer.plugins

import de.uniluebeck.itcr.termserver_loadbalancer.api.routeApi
import de.uniluebeck.itcr.termserver_loadbalancer.fhir.fhirApi
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        route("/api") {
            routeApi()
        }
        route("/fhir") {
            fhirApi()
        }
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}
