package de.uniluebeck.itcr.termserver_loadbalancer.ui

import de.uniluebeck.itcr.termserver_loadbalancer.ui.components.uiPage
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*


fun Route.uiRouting() {
    route("/ui") {
        get {
            call.respondHtml {
                uiPage()
            }
        }
    }
}
