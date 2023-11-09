package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import de.uniluebeck.itcr.termserver_loadbalancer.APP_NAME
import de.uniluebeck.itcr.termserver_loadbalancer.APP_VERSION
import kotlinx.html.BODY
import kotlinx.html.div
import kotlinx.html.h1

fun BODY.titleComponent() {
    div("container-fluid") {
        div("row") {
            div("col-12") {
                h1 { +"$APP_NAME version $APP_VERSION" }
            }
        }
    }
}