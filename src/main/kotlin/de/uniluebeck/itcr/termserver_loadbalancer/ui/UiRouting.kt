package de.uniluebeck.itcr.termserver_loadbalancer.ui

import de.uniluebeck.itcr.termserver_loadbalancer.APP_NAME
import de.uniluebeck.itcr.termserver_loadbalancer.APP_VERSION
import de.uniluebeck.itcr.termserver_loadbalancer.Storage
import de.uniluebeck.itcr.termserver_loadbalancer.models.LoadBalancingStrategy
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*


fun Route.uiRouting() {
    route("/ui") {
        get {
            call.respondHtml {
                head {
                    title { +"$APP_NAME version $APP_VERSION" }
                    link {
                        rel = LinkRel.stylesheet
                        href = "/static/css/bootstrap.min.css"
                    }
                    script {
                        src = "/static/js/bootstrap.min.js"
                    }
                }
                body {
                    div("container-fluid") {
                        div("row") {
                            div("col-12") {
                                h1 { +"$APP_NAME version $APP_VERSION" }
                            }
                        }
                    }

                    div("container-fluid") {
                        div("row") {
                            div("col-12") {
                                h2 { +"Configured servers" }
                            }
                        }
                        div("row") {
                            listOf("UUID" to 4, "URI" to 4, "Name" to 2, "Actions" to 2).forEach {
                                div("col-${it.second}") {
                                    b {
                                        +it.first
                                    }
                                }
                            }
                        }
                        Storage.endpoints.getEndpoints().forEach { endpoint ->
                            div("row") {
                                div("col-4") {
                                    +endpoint.uuid!!
                                }
                                div("col-4") {
                                    +(endpoint.uri.toString())
                                }
                                div("col-3") {
                                    +endpoint.name
                                }
                                div("col-1") {
                                    // TODO: add confirmation using JS?!
                                    form(
                                        method = FormMethod.post,
                                        encType = FormEncType.applicationXWwwFormUrlEncoded
                                    ) {
                                        action = "/api/endpoints/${endpoint.uuid}/delete"
                                        submitInput {
                                            value = "Delete"
                                        }
                                    }
                                }
                            }
                        }

                        form(
                            method = FormMethod.post,
                            action = "/api/endpoints",
                            encType = FormEncType.applicationXWwwFormUrlEncoded
                        ) {
                            div("row") {
                                div("col-4") {}
                                div("col-4") {
                                    input {
                                        name = "uri"
                                        type = InputType.url
                                        placeholder = "URI"
                                    }
                                }
                                div("col-3") {
                                    input {
                                        name = "name"
                                        type = InputType.text
                                        placeholder = "Name"
                                    }
                                }
                                div("col-1") {
                                    submitInput {
                                        value = "Add"
                                    }
                                }
                            }
                        }

                    }


                    div("container-fluid") {
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
                        form(method = FormMethod.post,
                            encType = FormEncType.multipartFormData,
                            action = "/api/load-balancing/readonly") {
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

                }
            }
        }
    }
}