package de.uniluebeck.itcr.termserver_loadbalancer.fhir

import ca.uhn.fhir.parser.IParser
import de.uniluebeck.itcr.termserver_loadbalancer.fhirContext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.hl7.fhir.r4b.model.DomainResource

val contentTypeFhirJson = ContentType.parse("application/fhir+json")
val contentTypeFhirXml = ContentType.parse("application/fhir+xml")

private val fhirJsonParser: IParser by lazy {
    fhirContext.newJsonParser().setPrettyPrint(true)
}
private val fhirXmlParser: IParser by lazy {
    fhirContext.newXmlParser().setPrettyPrint(true)
}

fun getParser(contentType: ContentType): IParser = when (contentType) {
    ContentType.Application.Json, contentTypeFhirJson -> fhirJsonParser
    ContentType.Application.Xml, contentTypeFhirXml -> fhirXmlParser
    else -> throw IllegalArgumentException("Unsupported content type $contentType")
}

fun validateFhirVersion(fhirVersionId: String?) = when (fhirVersionId) {
    null -> true
    "4.3" -> true //R4B
    "4.0" -> true //R4
    else -> false
}

fun validateFhirVersion(contentType: ContentType): Pair<Boolean, String?> = when (val fv = contentType.parameter("fhirVersion")) {
    null -> true to null
    else -> validateFhirVersion(fv) to fv
}

fun ApplicationCall.desiredFhirEncoding(throwOnError: Boolean = true): ContentType {
    val queryParamFormat = request.queryParameters["_format"]?.let { format ->
        when (format.lowercase()) {
            "json", ContentType.Application.Json.toString(), contentTypeFhirJson.toString() -> contentTypeFhirJson
            "xml", ContentType.Application.Xml.toString(), contentTypeFhirXml.toString() -> contentTypeFhirXml
            else -> {
                when (throwOnError) {
                    true -> throw IllegalArgumentException("Unsupported _format: $format")
                    else -> contentTypeFhirJson
                }
            }
        }
    }
    if (queryParamFormat != null) return queryParamFormat
    val accept = request.accept() ?: return contentTypeFhirJson
    val type = ContentType.parse(accept)
    return when (type.withoutParameters()) {
        ContentType.Application.Json, ContentType.Application.Xml -> type
        contentTypeFhirJson, contentTypeFhirXml -> type
        ContentType.Any -> contentTypeFhirJson
        else -> when (throwOnError) {
            true -> throw UnsupportedMediaTypeException(type)
            else -> contentTypeFhirJson
        }
    }
}

fun encodeResourceToDesiredFhirType(domainResource: DomainResource, contentType: ContentType): String {
    val parser = getParser(contentType)
    return parser.encodeResourceToString(domainResource)
}

suspend fun ApplicationCall.respondFhir(resource: DomainResource) {
    val requestedContentType = this.desiredFhirEncoding()
    // TODO: consider FHIR versions?
    validateFhirVersion(requestedContentType).let { (fhirVersionIsValid, fhirVersion) ->
        if (!fhirVersionIsValid) {
            throw FhirVersionUnsupportedException(fhirVersion)
        }
    }
    val encoded = encodeResourceToDesiredFhirType(resource, requestedContentType)
    this.respondText(encoded, requestedContentType)
}