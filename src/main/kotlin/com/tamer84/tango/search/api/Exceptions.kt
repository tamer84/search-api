package com.tamer84.tango.search.api

import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.tamer84.tango.search.config.EnvVar
import org.slf4j.MDC

class NotFoundException(message: String) : Exception(message)
class NotAuthorizedException(message: String = "Forbidden") : Exception(message)

fun Throwable.toApiError() : ApiError {
    return runCatching {
        val code = when(this) {
            is IllegalArgumentException, is NullPointerException, is ValueInstantiationException -> 400
            is IllegalStateException -> 500
            is NotAuthorizedException -> 403
            is NotFoundException -> 404
            else -> 500
        }
        val reqId = kotlin.runCatching { MDC.get(EnvVar.MDC_REQUEST_ID) }
        val message = this.cause?.message ?: this.message
        return ApiError(code, message, reqId.getOrDefault("not_available"))
    }.getOrDefault(ApiError(500))
}

