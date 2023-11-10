package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import de.uniluebeck.itcr.termserver_loadbalancer.Storage
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.EndpointRole
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.LoadBalancerState
import kotlinx.html.*

enum class SettingsColumns(val title: String, val width: Int) {
    ID("ID", 1),
    NAME("Name", 2),
    ROLE("Role", 3),
    READONLY("Read-only", 2),
    DISABLED("Disabled", 2),
}

fun BODY.loadBalancingConfigurationBlock() = div("container-fluid") {
    div("row") {
        div("col-12") {
            h2 { +"Load balancing" }
        }
    }
    val state = Storage.loadBalancerConf.loadBalancingState

    div("row") {
        div("col-12") {
            h4 { +"Endpoint roles" }
        }
    }
    div("row") {
        SettingsColumns.values().forEach {
            div("col-${it.width}") {
                b {
                    +it.title
                }
            }
        }
    }
    state.endpointIds.forEach { id ->
        val endpoint = Storage.endpoints.getEndpoints().firstOrNull { it.id == id }
            ?: throw IllegalStateException("Endpoint with ID $id not found")
        div("row") {
            div("col-${SettingsColumns.ID.width}") {
                +id
            }
            div("col-${SettingsColumns.NAME.width}") {
                +endpoint.name!!
            }
            div("col-${SettingsColumns.ROLE.width}") {
                roleChanger(state, id)
            }
            div("col-${SettingsColumns.READONLY.width}") {
                readonlyChanger(state, id)
            }
        }
    }
}

fun DIV.readonlyChanger(state: LoadBalancerState, configId: String) {
    div("form-check form-switch") {
        input(InputType.checkBox, classes = "form-check-input check-ro") {
            val htmlId = "check-ro-$configId"
            this.role = "switch"
            this.id = htmlId
            val endpointReadonly = state.endpointReadonlyMap[configId]!!
            if (endpointReadonly) {
                checked = true
            }
            label("form-check-label") {
                htmlFor = htmlId
                when (endpointReadonly) {
                    true -> +"R/O"
                    else -> +"R/W"
                }
            }
        }
    }
}

private fun DIV.roleChanger(
    state: LoadBalancerState,
    id: String
) {
    div("btn-group") {
        this.role = "group"
        val endpointRole = state.endpointRoleMap[id]!!
        button(classes = "btn make-unassigned ") {
            classes += when (endpointRole) {
                EndpointRole.UNASSIGNED -> "btn-dark disabled"
                else -> "btn-outline-dark"
            }
            type = ButtonType.button
            value = id
            +"Unassigned"
        }
        button(classes = "btn make-blue ") {
            classes += when (endpointRole) {
                EndpointRole.BLUE -> "btn-primary disabled"
                else -> "btn-outline-primary"
            }
            type = ButtonType.button
            value = id
            +"Blue"
        }
        button(classes = "btn make-green ") {
            classes += when (endpointRole) {
                EndpointRole.GREEN -> "btn-success disabled"
                else -> "btn-outline-success"
            }
            type = ButtonType.button
            value = id
            +"Green"
        }
    }
}
