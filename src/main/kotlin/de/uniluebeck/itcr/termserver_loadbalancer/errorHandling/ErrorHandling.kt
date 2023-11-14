package de.uniluebeck.itcr.termserver_loadbalancer.errorHandling

import de.uniluebeck.itcr.termserver_loadbalancer.api.ApiException
import de.uniluebeck.itcr.termserver_loadbalancer.fhir.FhirVersionUnsupportedException
import de.uniluebeck.itcr.termserver_loadbalancer.fhir.desiredFhirEncoding
import de.uniluebeck.itcr.termserver_loadbalancer.fhir.encodeResourceToDesiredFhirType
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import de.uniluebeck.itcr.termserver_loadbalancer.configstorage.EndpointSettingsError
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.OurMetrics
import de.uniluebeck.itcr.termserver_loadbalancer.plugins.OurMetrics.Companion.appMicrometerRegistry
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.hl7.fhir.r4b.model.CodeableConcept
import org.hl7.fhir.r4b.model.OperationOutcome

fun Application.configureErrorHandling() {
    suspend fun handlerForException(call: ApplicationCall, cause: Throwable) {
        val (operationOutcome, statusCode) = generateOperationOutcome(cause)
        val encoding = call.desiredFhirEncoding(throwOnError = false)
        val encoded = encodeResourceToDesiredFhirType(operationOutcome, encoding)
        logger.error("Application error, status code $statusCode; exception: ${cause.message}")
        call.respondText(text = encoded, contentType = encoding, status = statusCode)
    }
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, _ ->
            val notFound = NotFoundException("Resource not found at ${call.request.uri}")
            handlerForException(call, notFound)
        }
        exception<Throwable> { call, cause ->
            handlerForException(call, cause)
        }
    }
}



fun generateOperationOutcome(cause: Throwable): Pair<OperationOutcome, HttpStatusCode> {
    val (issueCode, httpStatus) = when (cause) {
        is FhirVersionUnsupportedException -> OperationOutcome.IssueType.NOTSUPPORTED to HttpStatusCode.BadRequest
        is UnsupportedContentTypeException, is UnsupportedMediaTypeException -> OperationOutcome.IssueType.INVALID to HttpStatusCode.UnsupportedMediaType
        is NotImplementedError -> OperationOutcome.IssueType.NOTSUPPORTED to HttpStatusCode.NotImplemented
        is EndpointSettingsError -> OperationOutcome.IssueType.INVALID to HttpStatusCode.ExpectationFailed
        is ApiException -> cause.issueType to cause.statusCode
        is IllegalStateException -> OperationOutcome.IssueType.UNKNOWN to HttpStatusCode.InternalServerError
        is NotFoundException -> OperationOutcome.IssueType.NOTFOUND to HttpStatusCode.NotFound
        else -> OperationOutcome.IssueType.UNKNOWN to HttpStatusCode.InternalServerError
    }
    val operationOutcome = OperationOutcome().apply {
        addIssue(OperationOutcome.OperationOutcomeIssueComponent().apply {
            severity = OperationOutcome.IssueSeverity.FATAL
            code = issueCode
            details = CodeableConcept().apply {
                text = cause::class.simpleName
            }
            diagnostics = cause.message
        })
    }
    appMicrometerRegistry.counter("errors.operationoutcome", "issueCode", issueCode.toCode()).increment()
    return operationOutcome to httpStatus
}