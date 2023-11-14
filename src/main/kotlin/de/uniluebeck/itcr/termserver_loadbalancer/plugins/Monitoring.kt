package de.uniluebeck.itcr.termserver_loadbalancer.plugins

import de.uniluebeck.itcr.termserver_loadbalancer.plugins.OurMetrics.Companion.appMicrometerRegistry
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.event.Level

private val ignoreLoggingPrefixes = listOf("static", "metrics", "favicon", "ui", "apple-touch-icon")

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            ignoreLoggingPrefixes.none { prefix ->
                call.request.path().trimStart('/').startsWith(prefix)
            }
        }
        callIdMdc("call-id")
    }
    install(CallId) {
        generate(8, "abcdef1234567890")
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }

    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
        metricName = "lb"
        meterBinders = emptyList()
    }
    routing {
        get("/metrics") {
            call.respond(appMicrometerRegistry.scrape())
        }
    }
}

class OurMetrics {
    companion object {
        val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
            config().commonTags("application", "tslb")
        }
    }
}
