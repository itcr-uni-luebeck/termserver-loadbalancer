package de.uniluebeck.itcr.termserver_loadbalancer

import ca.uhn.fhir.context.FhirContext
import de.uniluebeck.itcr.termserver_loadbalancer.errorHandling.configureErrorHandling
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.configureHTTP
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.configureMonitoring
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.configureRouting
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.configureSerialization
import de.uniluebeck.itcr.termserver_loadbalancer.ssl.configureTls
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("de.uniluebeck.itcr.Application")
val fhirContext: FhirContext by lazy { FhirContext.forR4B() }

const val APP_NAME = "TermServer LoadBalancer"
const val APP_VERSION = "0.1.0"

fun main() {
    val environment = applicationEngineEnvironment {
        //log = logger
        connector {
            port = System.getenv("TERMSERVER_LOADBALANCER_PORT")?.toInt() ?: 8080
            logger.info("Binding to port $port")
        }
        configureTls()
        module(Application::module)
    }
    embeddedServer(Netty, environment).start(wait = true)
}

fun Application.module() {
    configureMonitoring()
    configureSerialization()
    //configureTemplating()
    configureHTTP()
    configureRouting()
    configureErrorHandling()
}

