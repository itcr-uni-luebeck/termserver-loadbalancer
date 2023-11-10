package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import de.uniluebeck.itcr.termserver_loadbalancer.Storage
import kotlinx.html.*

private enum class TableColumn(val title: String, val width: Int) {
    UUID("ID", 1),
    METADATA("Metadata", 3),
    URI("URI", 4),
    NAME("Name", 2),
    ACTIONS("Actions", 2)
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

private fun DIV.contentRows() {
    Storage.endpoints.getEndpoints().forEach { endpoint ->
        div("row endpoint-table") {
            div("col-${TableColumn.UUID.width}") {
                +endpoint.id!!
            }
            div("col-${TableColumn.METADATA.width}") {
                val formattedDetails = listOfNotNull(
                    endpoint.endpointDetails?.fhirVersion,
                    endpoint.endpointDetails?.softwareName,
                    endpoint.endpointDetails?.softwareVersion
                ).joinToString(", ")
                +formattedDetails
            }
            div("col-${TableColumn.URI.width}") {
                a(endpoint.uri.toString()) {
                    +(endpoint.uri.toString())
                }
            }
            div("col-${TableColumn.NAME.width}") {
                +endpoint.name!!
            }
            div("col-${TableColumn.ACTIONS.width}") {
                button(classes = "endpoint-delete-button btn btn-danger") {
                    value = endpoint.id!!
                    +"Delete"
                }
            }
        }
    }
}

private fun DIV.addForm() {
    form(
        method = FormMethod.get,
        action = ""
    ) {
        id = "addForm"
        div("row") {
            div("col-${TableColumn.UUID.width}") {}
            div("col-${TableColumn.METADATA.width}") {
                i {
                    span {
                        id = "newItemMetadata"
                    }
                }
            }
            div("col-${TableColumn.URI.width}") {
                input {
                    name = "uri"
                    type = InputType.url
                    placeholder = "URI"
                    required = true
                    id = "inputUrl"
                }
            }
            div("col-${TableColumn.NAME.width}") {
                input {
                    name = "name"
                    type = InputType.text
                    placeholder = "Name"
                    required = true
                    id = "inputName"
                }
            }
            div("col-${TableColumn.ACTIONS.width}") {
                submitInput(classes = "btn btn-primary") {
                    value = "Add"
                }
            }
        }
    }
}
