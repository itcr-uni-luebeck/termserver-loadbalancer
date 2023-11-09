package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.head

fun HTML.uiPage() {
    head {
        htmlHeader()
    }
    body {
        titleComponent()
        configuredServersBlock()
        loadBalancingConfigurationBlock()
    }
}