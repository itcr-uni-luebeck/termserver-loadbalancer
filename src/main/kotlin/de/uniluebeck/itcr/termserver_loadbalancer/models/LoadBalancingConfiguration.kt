package de.uniluebeck.itcr.termserver_loadbalancer.models

import de.uniluebeck.itcr.termserver_loadbalancer.JsonBackedStorage
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.endpoints
import de.uniluebeck.itcr.termserver_loadbalancer.Storage.Companion.json
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class LoadBalancingState(
    val strategy: LoadBalancingStrategy, val readOnlyModeEnabled: Boolean, val endpointMap: Map<String, EndpointStatus>
)

class LoadBalancerConfiguration : JsonBackedStorage<LoadBalancingState>() {
    fun addEndpoint(endpointUuid: String) {
        loadBalancingState = loadBalancingState.copy(
            strategy = when (loadBalancingState.endpointMap.size + 1) {
                1 -> LoadBalancingStrategy.ONLY_ONE_SERVER
                else -> when (loadBalancingState.strategy) {
                    LoadBalancingStrategy.ONLY_ONE_SERVER -> LoadBalancingStrategy.defaultStrategy()
                    else -> loadBalancingState.strategy
                }
            },
            endpointMap = loadBalancingState.endpointMap.toMutableMap().apply {
                this[endpointUuid] = EndpointStatus.ACTIVE
            }
        )
        writeToStorage(loadBalancingState)
    }
    fun removeEndpoint(endpointUuid: String) {
        loadBalancingState = loadBalancingState.copy(
            strategy = when (loadBalancingState.endpointMap.size - 1) {
                1 -> LoadBalancingStrategy.ONLY_ONE_SERVER
                else -> loadBalancingState.strategy
            },
            endpointMap = loadBalancingState.endpointMap.filterKeys { it != endpointUuid }
        )
        writeToStorage(loadBalancingState)
    }

    var loadBalancingState: LoadBalancingState = when (fileLocation.exists()) {
        true -> {
            val jsonString = fileLocation.readText()
            json.decodeFromString(jsonString)
        }

        else -> {
            logger.info("No endpoints.json found, creating new empty file")
            val strategy = when (endpoints.getEndpoints().size) {
                0 -> LoadBalancingStrategy.DOWNTIME
                1 -> LoadBalancingStrategy.ONLY_ONE_SERVER
                else -> LoadBalancingStrategy.defaultStrategy()
            }
            LoadBalancingState(strategy = strategy,
                readOnlyModeEnabled = false,
                endpointMap = endpoints.getEndpoints().mapNotNull { it.uuid }
                    .associateWith { EndpointStatus.ACTIVE }).also {
                writeToStorage(it)
            }
        }
    }
}

enum class LoadBalancingStrategy {
    ROUND_ROBIN, RANDOM, BLUE_GREEN, ONLY_ONE_SERVER, DOWNTIME;
    companion object {
        fun defaultStrategy(): LoadBalancingStrategy = ROUND_ROBIN
    }
}

enum class EndpointStatus {
    ACTIVE, INACTIVE
}