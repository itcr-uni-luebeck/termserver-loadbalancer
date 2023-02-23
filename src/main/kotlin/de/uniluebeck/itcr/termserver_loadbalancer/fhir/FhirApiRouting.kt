package de.uniluebeck.itcr.termserver_loadbalancer.fhir

import de.uniluebeck.itcr.termserver_loadbalancer.LoadBalancer
import de.uniluebeck.itcr.termserver_loadbalancer.Storage
import de.uniluebeck.itcr.termserver_loadbalancer.fhirContext
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.hl7.fhir.r4b.model.*

private val fhirParser by lazy {
    fhirContext.newJsonParser().setPrettyPrint(true)
}

fun Route.fhirApi() {
    get("/metadata") {
        logger.info("FHIR GET request to metadata")
        val capabilityStatementString = fhirParser.encodeResourceToString(generateCapabilityStatement())
        call.respond(capabilityStatementString)
    }

    get("/{...fhirLocation}") {
        val fhirLocation = call.parameters["...fhirLocation"] ?: ""
        logger.info("FHIR GET request to $fhirLocation")
        val desiredEncoding = call.request.accept() ?: "application/json"
        val (balancedResponse, downstreamUrl) = LoadBalancer.requestGet(fhirLocation, desiredEncoding)
        logger.info(balancedResponse.toString())
        call.response.header("X-Downstream-URL", downstreamUrl.toString())
        call.respondText(balancedResponse.bodyAsText(), balancedResponse.contentType(), balancedResponse.status)
    }

}

private fun generateCapabilityStatement(): CapabilityStatement = CapabilityStatement().apply {
    fhirVersion = Enumerations.FHIRVersion._4_3_0
    version = "0.1.0"
    name = "FHIR_Conformance_statement_for_${APP_NAME.replace(' ', '_')}"
    title = "FHIR Conformance statement for $APP_NAME"
    status = Enumerations.PublicationStatus.ACTIVE
    experimental = true
    dateElement = DateTimeType.now()
    publisher = "SU TermServ"
    contact.add(ContactDetail().apply {
        name = "SU-TermServ"
        telecom.add(ContactPoint().apply {
            system = ContactPoint.ContactPointSystem.EMAIL
            value = "team@mail.mii-termserv.de"
        })
        telecom.add(ContactPoint().apply {
            system = ContactPoint.ContactPointSystem.URL
            value = "https://mii-termserv.de"
        })
    })
    description =
        "An instance of $APP_NAME that provides access to the FHIR API of multiple FHIR Terminology Servers."
    software = CapabilityStatement.CapabilityStatementSoftwareComponent().apply {
        name = APP_NAME
        version = APP_VERSION
    }
    kind = Enumerations.CapabilityStatementKind.INSTANCE
    addFormat("application/json")
    addFormat("application/xml")

    Storage.endpoints.getEndpoints().forEach {
        val downstreamMetadata = it.resolveUri("metadata")
        addInstantiates(downstreamMetadata.toString())
    }

    rest.add(CapabilityStatement.CapabilityStatementRestComponent().apply {
        // TODO: add resource definitions for TS
        mode = CapabilityStatement.RestfulCapabilityMode.SERVER
        val supportedResourceTypes = listOf(
            "CodeSystem",
            "ConceptMap",
            "ValueSet",
            "NamingSystem",
            "Bundle"
        )
        supportedResourceTypes.forEach { resourceType ->
            resource.add(CapabilityStatement.CapabilityStatementRestResourceComponent().apply {
                type = resourceType
                versioning = CapabilityStatement.ResourceVersionPolicy.VERSIONEDUPDATE
            })
        }
    })
}

const val APP_NAME = "TermServer LoadBalancer"
const val APP_VERSION = "0.1.0"