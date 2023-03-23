package de.uniluebeck.itcr.termserver_loadbalancer.ui

import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Route.staticRouting() {
    static("/static") {
        resources("static")
        files(".")
    }
    static("/openapi") {
        staticRootFolder = File("src/main/resources/openapi")
        files(".")
    }
}