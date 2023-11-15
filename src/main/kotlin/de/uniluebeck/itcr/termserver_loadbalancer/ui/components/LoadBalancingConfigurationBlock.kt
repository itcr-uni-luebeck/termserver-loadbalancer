package de.uniluebeck.itcr.termserver_loadbalancer.ui.components

import de.uniluebeck.itcr.termserver_loadbalancer.Storage
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.EndpointRole
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.LoadBalancerState
import kotlinx.html.*

enum class SettingsColumns(val title: String, val width: Int) {
    ID("ID", 1),
    NAME("Name", 2),
    ROLE("Role", 3),
    READONLY("Read-only", 1),
    ON_OFF("On/Off", 1),
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
        SettingsColumns.entries.forEach {
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
            div("col-${SettingsColumns.ON_OFF.width}") {
                onOffChanger(state, id)
            }
        }
    }

    div("row") {
        div("col-12") {
            h4 { +"System configuration" }
        }
    }


    systemConfigurationBlock()

}

private fun DIV.systemConfigurationBlock() {
    div("row") {

    }
}

fun DIV.toggleChanger(
    state: LoadBalancerState,
    configId: String,
    booleanGetter: (LoadBalancerState) -> Boolean,
    labels: Map<Boolean, String>,
    inputClass: String
) {
    div("form-check form-switch") {
        input(type = InputType.checkBox, classes = "form-check-input $inputClass") {
            val htmlId = "$inputClass-$configId"
            this.id = htmlId
            this.role = "switch"
            this.value = configId
            val valueIsSet = booleanGetter(state)
            checked = valueIsSet
            label("form-check-label") {
                htmlFor = htmlId
                val labelValue = labels[valueIsSet]!!
                +labelValue
            }
        }
    }
}

fun DIV.readonlyChanger(state: LoadBalancerState, configId: String) = toggleChanger(
    state = state, configId = configId, booleanGetter = {
        it.endpointReadonlyMap[configId]!!
    },
    labels = mapOf(false to "R/O", true to "R/W"),
    inputClass = "check-ro"
)

fun DIV.onOffChanger(state: LoadBalancerState, configId: String) = toggleChanger(
    state = state, configId = configId, booleanGetter = {
        it.endpointStatusMap[configId]!!
    },
    labels = mapOf(false to "OFF", true to "ON"),
    inputClass = "check-on-off"
)

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
