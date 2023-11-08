package de.uniluebeck.itcr.termserver_loadbalancer.errorHandling

import de.uniluebeck.itcr.termserver_loadbalancer.fhir.FhirVersionUnsupportedException
import de.uniluebeck.itcr.termserver_loadbalancer.fhir.desiredFhirEncoding
import de.uniluebeck.itcr.termserver_loadbalancer.fhir.encodeResourceToDesiredFhirType
import de.uniluebeck.itcr.termserver_loadbalancer.logger
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.hl7.fhir.r4b.model.OperationOutcome

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val (operationOutcome, statusCode) = generateOperationOutcome(cause)
            val encoding = call.desiredFhirEncoding(throwOnError = false)
            val encoded = encodeResourceToDesiredFhirType(operationOutcome, encoding)
            logger.error("Application error, status code $statusCode; exception: ${cause.message}")
            call.respondText(text = encoded, contentType = encoding, status = statusCode)
        }
    }
}



fun generateOperationOutcome(cause: Throwable): Pair<OperationOutcome, HttpStatusCode> {
    val (issueCode, httpStatus) = when (cause) {
        is FhirVersionUnsupportedException -> OperationOutcome.IssueType.NOTSUPPORTED to HttpStatusCode.BadRequest
        is UnsupportedContentTypeException, is UnsupportedMediaTypeException -> OperationOutcome.IssueType.INVALID to HttpStatusCode.UnsupportedMediaType
        is NotImplementedError -> OperationOutcome.IssueType.NOTSUPPORTED to HttpStatusCode.NotImplemented
        else -> OperationOutcome.IssueType.EXCEPTION to HttpStatusCode.InternalServerError
    }
    val operationOutcome = OperationOutcome().apply {
        addIssue(OperationOutcome.OperationOutcomeIssueComponent().apply {
            severity = OperationOutcome.IssueSeverity.FATAL
            code = issueCode
            diagnostics = cause.message
        })
    }
    return operationOutcome to httpStatus
}