package de.uniluebeck.itcr

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import de.uniluebeck.itcr.plugins.*
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("de.uniluebeck.itcr.Application")


fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureMonitoring()
    configureSerialization()
    configureTemplating()
    configureHTTP()
    configureRouting()
}
