package de.uniluebeck.itcr.termserver_loadbalancer.client

import de.uniluebeck.itcr.termserver_loadbalancer.fhirContext
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.Endpoint
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.hl7.fhir.r4b.model.DomainResource

class FhirAwareClient {
    companion object {
        suspend inline fun <reified T : DomainResource> getDomainResource(endpoint: Endpoint, path: String): T {
            val uri = endpoint.resolveUri(path)
            val response = HttpClient.client.get(uri.toURL()) {
                header("Accept", "application/fhir+json, application/json")
            }
            logger.info("Requested from $uri")
            val responseString = response.bodyAsText()
            val jsonParser = fhirContext.newJsonParser()
            val resource = jsonParser.parseResource(responseString) as DomainResource
            return when {
                resource is T -> resource
                else -> throw Exception("Resource is not of type ${T::class.simpleName}")
            }
        }
    }

}