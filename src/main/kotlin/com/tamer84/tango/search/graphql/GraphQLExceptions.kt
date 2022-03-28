package com.tamer84.tango.search.graphql

import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import com.tamer84.tango.search.api.toApiError
import org.slf4j.LoggerFactory

class CustomExceptionHandler : DataFetcherExceptionHandler {

    companion object {
        private val log = LoggerFactory.getLogger(CustomExceptionHandler::class.java)
    }

    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters?): DataFetcherExceptionHandlerResult {
        val exception = handlerParameters!!.exception

        val apiError = exception.cause?.toApiError() ?: exception.toApiError()

        log.error("Request Failed [$apiError]", exception);

        val error = GraphqlErrorBuilder
            .newError()
            .path(handlerParameters.path)
            .message(apiError.message)
            .extensions(mapOf(
                "code" to apiError.status,
                "msg" to apiError.message,
                "reqId" to apiError.reqId)
            )
            .build()

        return DataFetcherExceptionHandlerResult
            .newResult()
            .error(error)
            .build()
    }

}
