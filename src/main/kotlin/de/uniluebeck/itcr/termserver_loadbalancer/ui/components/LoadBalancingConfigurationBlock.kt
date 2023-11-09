package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import de.uniluebeck.itcr.termserver_loadbalancer.Storage
import de.uniluebeck.itcr.termserver_loadbalancer.models.LoadBalancingStrategy
import kotlinx.html.*

fun BODY.loadBalancingConfigurationBlock() = div("container-fluid") {
    div("row") {
        div("col-12") {
            h2 { +"Load balancing" }
        }
    }
    val state = Storage.loadBalancerConf.loadBalancingState
    form(
        method = FormMethod.post,
        encType = FormEncType.multipartFormData,
        action = "/api/load-balancing/strategy"
    ) {
        div("row") {
            div("col-3") {
                +"Load balancing strategy"
            }
            div("col-1") {
                +state.strategy.toString()
            }

            div("col-2") {
                select {
                    name = "strategy"
                    option {
                        value = "round-robin"
                        selected = state.strategy == LoadBalancingStrategy.ROUND_ROBIN
                        +"Round-robin"
                    }
                    option {
                        value = "random"
                        selected = state.strategy == LoadBalancingStrategy.RANDOM
                        +"Random"
                    }
                }
            }
            div("col-2") {
                submitInput {
                    value = "Set strategy"
                }
            }
        }
    }
    form(
        method = FormMethod.post,
        encType = FormEncType.multipartFormData,
        action = "/api/load-balancing/readonly"
    ) {
        div("row") {
            div("col-3") {
                +"Read-only mode"
            }
            div("col-1") {
                +state.readOnlyModeEnabled.toString()
            }

            div("col-2") {
                select {
                    name = "readonly"
                    option {
                        value = "true"
                        selected = state.readOnlyModeEnabled
                        +"Enabled"
                    }
                    option {
                        value = "false"
                        selected = !state.readOnlyModeEnabled
                        +"Disabled"
                    }
                }
            }
            div("col-2") {
                submitInput { value = "Set readonly mode" }
            }
        }
    }

}
