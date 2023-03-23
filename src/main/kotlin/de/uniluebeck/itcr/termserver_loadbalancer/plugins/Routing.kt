package de.uniluebeck.itcr.termserver_loadbalancer.plugins

import de.uniluebeck.itcr.termserver_loadbalancer.api.routeApi
import de.uniluebeck.itcr.termserver_loadbalancer.fhir.fhirApi
import de.uniluebeck.itcr.termserver_loadbalancer.ui.uiRouting
import de.uniluebeck.itcr.termserver_loadbalancer.ui.staticRouting
import de.uniluebeck.itcr.termserver_loadbalancer.ui.swaggerRouting
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.application.*

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
        uiRouting()
        staticRouting()
        swaggerRouting()
        route("/") {
            get {
                call.respondRedirect("/ui")
            }
        }
    }
}
