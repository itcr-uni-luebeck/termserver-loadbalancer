package de.uniluebeck.itcr.termserver_loadbalancer.configstorage

import de.uniluebeck.itcr.termserver_loadbalancer.JsonBackedStorage
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.endpoints
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.json
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import kotlinx.serialization.Serializable

@Serializable
data class LoadBalancerState(
    val readOnlyModeEnabled: Boolean,
    val activeRole: EndpointRole,
    val endpointIds: List<String>,
    val endpointStatusMap: Map<String, Boolean>,
    val endpointRoleMap: Map<String, EndpointRole>,
    val endpointReadonlyMap: Map<String, Boolean>,
)

class LoadBalancerConfiguration : JsonBackedStorage<LoadBalancerState>() {
    var loadBalancingState: LoadBalancerState = initializeConfig()

    private fun initializeConfig(): LoadBalancerState {
        return when (fileLocation.exists()) {
            true -> {
                try {
                    val jsonString = fileLocation.readText()
                    val decodedConfiguration = json.decodeFromString<LoadBalancerState>(jsonString)
                    val (verified, reason) = verifyConfiguration(decodedConfiguration)
                    if (verified) {
                        return decodedConfiguration
                    } else {
                        logger.error("Could not verify configuration (offending: $reason), creating new default configuration")
                        createDefaultConfiguration().also {
                            writeToStorage(it)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Could not read configuration, creating new default configuration", e)
                    createDefaultConfiguration().also {
                        writeToStorage(it)
                    }
                }
            }

            else -> createDefaultConfiguration().also {
                writeToStorage(it)
            }
        }
    }

    private fun verifyConfiguration(decodedConfiguration: LoadBalancerState): Pair<Boolean, String?> {
        val configuredIds = endpoints.getEndpoints().map {
            it.id
        }.toSet()
        if (decodedConfiguration.endpointIds.toSet() != configuredIds) {
            return false to "IDs"
        }
        if (decodedConfiguration.endpointRoleMap.keys.toSet() != configuredIds) {
            return false to "roles"
        }
        if (decodedConfiguration.endpointReadonlyMap.keys.toSet() != configuredIds) {
            return false to "Readonly"
        }
        if (decodedConfiguration.endpointStatusMap.keys.toSet() != configuredIds) {
            return false to "Status"
        }

        return true to null
    }

    private fun createDefaultConfiguration(): LoadBalancerState {
        logger.info("No endpoints.json found, creating new empty file")
        val endpointUuids = endpoints.getEndpoints().mapNotNull { it.id }
        val newState = LoadBalancerState(
            readOnlyModeEnabled = false,
            activeRole = EndpointRole.BLUE,
            endpointIds = endpointUuids,
            endpointStatusMap = endpointUuids.associateWith { true },
            endpointRoleMap = endpointUuids.associateWith { EndpointRole.UNASSIGNED },
            endpointReadonlyMap = endpointUuids.associateWith { false })
        return newState.also {
            writeToStorage(it)
        }
    }

    private fun setActiveConfiguration(conf: LoadBalancerState): LoadBalancerState {
        writeToStorage(conf)
        this.loadBalancingState = conf
        return conf
    }

    fun addEndpoint(endpoint: Endpoint): LoadBalancerState {
        val id = endpoint.id ?: throw EndpointSettingsError("Endpoint ID must not be null")
        val newState = loadBalancingState.copy(
            endpointIds = loadBalancingState.endpointIds + id,
            endpointStatusMap = loadBalancingState.endpointStatusMap + (id to true),
            endpointRoleMap = loadBalancingState.endpointRoleMap + (id to EndpointRole.UNASSIGNED),
            endpointReadonlyMap = loadBalancingState.endpointReadonlyMap + (id to false)
        )
        return setActiveConfiguration(newState)
    }

    fun designateEndpointAs(endpointId: String, role: EndpointRole): LoadBalancerState {
        val newState = loadBalancingState.copy(
            endpointRoleMap = loadBalancingState.endpointRoleMap + (endpointId to role)
        )
        return setActiveConfiguration(newState)
    }

    fun readonlyEndpoint(endpointId: String, readonly: Boolean): LoadBalancerState {
        val newState = loadBalancingState.copy(
            endpointReadonlyMap = loadBalancingState.endpointReadonlyMap + (endpointId to readonly)
        )
        return setActiveConfiguration(newState)
    }

    fun statusEndpoint(endpointId: String, status: Boolean): LoadBalancerState {
        val newState = loadBalancingState.copy(
            endpointStatusMap = loadBalancingState.endpointStatusMap + (endpointId to status)
        )
        return setActiveConfiguration(newState)
    }

    fun removeEndpoint(endpointId: String): LoadBalancerState {
        val newState = loadBalancingState.copy(
            endpointIds = loadBalancingState.endpointIds - endpointId,
            endpointStatusMap = loadBalancingState.endpointStatusMap - endpointId,
            endpointRoleMap = loadBalancingState.endpointRoleMap - endpointId,
            endpointReadonlyMap = loadBalancingState.endpointReadonlyMap - endpointId
        )
        return setActiveConfiguration(newState)
    }

    fun setActiveRole(role: EndpointRole): LoadBalancerState {
        if (role == EndpointRole.UNASSIGNED) throw EndpointSettingsError("Cannot set role to UNASSIGNED")
        val newState = loadBalancingState.copy(
            activeRole = role
        )
        return setActiveConfiguration(newState)
    }
}

enum class EndpointRole {
    UNASSIGNED, BLUE, GREEN
}