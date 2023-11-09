package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import de.uniluebeck.itcr.termserver_loadbalancer.Storage
import kotlinx.html.*

private enum class TableColumn(val title: String, val width: Int) {
    UUID("UUID", 3),
    METADATA("Metadata", 3),
    URI("URI", 3),
    NAME("Name", 2),
    ACTIONS("Actions", 1)
}

fun BODY.configuredServersBlock() {
    div("container-fluid") {
        div("row") {
            div("col-12") {
                h2 { +"Configured servers" }
            }
        }
        div("row") {
            TableColumn.values().forEach {
                div("col-${it.width}") {
                    b {
                        +it.title
                    }
                }
            }
        }
        contentRows()
        addForm()
    }
}

private fun DIV.addForm() {
    form(
        method = FormMethod.post,
        action = "/api/endpoints",
        encType = FormEncType.applicationXWwwFormUrlEncoded
    ) {
        div("row") {
            listOf(TableColumn.UUID, TableColumn.METADATA).forEach {
                div("col-${it.width}") {}
            }
            div("col-${TableColumn.URI.width}") {
                input {
                    name = "uri"
                    type = InputType.url
                    placeholder = "URI"
                }
            }
            div("col-${TableColumn.NAME.width}") {
                input {
                    name = "name"
                    type = InputType.text
                    placeholder = "Name"
                }
            }
            div("col-${TableColumn.ACTIONS.width}") {
                submitInput {
                    value = "Add"
                }
            }
        }
    }
}

private fun DIV.contentRows() = Storage.endpoints.getEndpoints().forEach { endpoint ->
    div("row") {
        div("col-${TableColumn.UUID.width}") {
            +endpoint.uuid!!
        }
        div("col-${TableColumn.METADATA.width}") {
            val formattedDetails = listOfNotNull(
                endpoint.endpointDetails?.fhirVersion?.let { "FHIR $it" },
                endpoint.endpointDetails?.softwareName,
                endpoint.endpointDetails?.softwareVersion
            ).joinToString(", ")
            +formattedDetails
        }
        div("col-${TableColumn.URI.width}") {
            +(endpoint.uri.toString())
        }
        div("col-${TableColumn.NAME.width}") {
            +endpoint.name
        }
        div("col-${TableColumn.ACTIONS.width}") {
            // TODO: use AJAX?
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