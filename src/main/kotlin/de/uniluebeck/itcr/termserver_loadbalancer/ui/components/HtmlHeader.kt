package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import de.uniluebeck.itcr.termserver_loadbalancer.APP_NAME
import de.uniluebeck.itcr.termserver_loadbalancer.APP_VERSION
import kotlinx.html.*

fun HEAD.htmlHeader() {
    title { +"$APP_NAME version $APP_VERSION" }
    link {
        rel = LinkRel.stylesheet
        href = "/static/css/bootstrap.min.css"
    }
    script {
        src = "/static/js/bootstrap.min.js"
    }
}