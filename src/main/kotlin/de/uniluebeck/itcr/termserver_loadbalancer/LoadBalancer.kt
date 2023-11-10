package de.uniluebeck.itcr.termserver_loadbalancer

class LoadBalancer {
    companion object {

//        private val activeEndpoints get() = loadBalancerConf.loadBalancingState.endpointMap.filter { it.value == EndpointStatus.ACTIVE }.map { it.key }
//        private val numberOfActiveEndpoints get() = activeEndpoints.size
//
//        private var roundRobinIndex = 0
//        suspend fun requestGet(fhirLocation: String, desiredEncoding: ContentType) : Pair<HttpResponse, URI> {
//            val targetServerId = when(val strategy = loadBalancerConf.loadBalancingState.strategy) {
//                LoadBalancingStrategy.ONLY_ONE_SERVER -> activeEndpoints.toList().first()
//                LoadBalancingStrategy.ROUND_ROBIN -> activeEndpoints.toList()[roundRobinIndex].also {
//                    roundRobinIndex = if (roundRobinIndex + 1 >= numberOfActiveEndpoints) 0 else roundRobinIndex + 1
//                }
//                LoadBalancingStrategy.RANDOM -> activeEndpoints.toList().random()
//                else -> TODO("Not yet implemented: $strategy")
//            }
//            val targetServer = Storage.endpoints.getEndpoints().first { it.id == targetServerId }
//            val targetUrl = targetServer.resolveUri(fhirLocation)
//            return HttpClient.client.get(targetUrl.toURL()) {
//                header("Accept", desiredEncoding)
//            } to targetUrl
//        }
//    }
    }

}