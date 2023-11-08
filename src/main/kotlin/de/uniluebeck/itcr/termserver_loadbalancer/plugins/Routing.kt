package de.uniluebeck.itcr.termserver_loadbalancer.plugins

import de.uniluebeck.itcr.termserver_loadbalancer.api.routeApi
import de.uniluebeck.itcr.termserver_loadbalancer.fhir.fhirApi
import de.uniluebeck.itcr.termserver_loadbalancer.ui.staticRouting
import de.uniluebeck.itcr.termserver_loadbalancer.ui.swaggerRouting
import de.uniluebeck.itcr.termserver_loadbalancer.ui.uiRouting
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
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