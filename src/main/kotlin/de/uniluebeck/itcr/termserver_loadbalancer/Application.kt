package de.uniluebeck.itcr.termserver_loadbalancer

import ca.uhn.fhir.context.FhirContext
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("de.uniluebeck.itcr.Application")
val fhirContext by lazy { FhirContext.forR4B() }

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
