package de.uniluebeck.itcr.plugins

import de.uniluebeck.itcr.api.routeApi
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
            this.routeApi()
        }
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
