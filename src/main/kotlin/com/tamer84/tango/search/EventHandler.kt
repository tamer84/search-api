package com.tamer84.tango.search

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.tamer84.tango.search.api.ApiGatewayResponse
import com.tamer84.tango.search.api.toApiError
import com.tamer84.tango.search.config.EnvVar
import com.tamer84.tango.search.graphql.GraphQLRequest
import com.tamer84.tango.search.graphql.GraphQLService
import com.tamer84.tango.search.util.IocContainer
import com.tamer84.tango.search.util.JsonUtil
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.atomic.AtomicLong


// https://github.com/rupakg/aws-java-products-api/blob/master/src/main/java/com/serverless/ApiGatewayResponse.java
// https://cloudonaut.io/review-api-gateway-http-apis/
// https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html
class EventHandler(private val graphQLService: GraphQLService = IocContainer.graphQLService): RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

    companion object {
        private val log = LoggerFactory.getLogger(EventHandler::class.java)

        private val HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"
        private val HEADER_REQUEST_ID = "x-tango-request-id"
        private val HEADER_RESPONSE_TIME = "x-tango-response-time"

        private val OPTIONS_HEADERS = mapOf(
            "Access-Control-Allow-Headers" to "*",
            HEADER_ACCESS_CONTROL_ALLOW_ORIGIN to "*",
            "Access-Control-Allow-Methods" to "OPTIONS,POST,GET"
        )

        private val fnCounter = AtomicLong()
    }

    override fun handleRequest(event: APIGatewayProxyRequestEvent, ctx: Context): ApiGatewayResponse {

        if(event.httpMethod == "OPTIONS") {
            return ApiGatewayResponse(headers = OPTIONS_HEADERS)
        }

        MDC.put(EnvVar.MDC_REQUEST_ID, ctx.awsRequestId)
        log.info("Received API request [fnCount={}]", fnCounter.incrementAndGet())

        try {

            // prepare
            val start = System.currentTimeMillis()
            val graphRequest = resolveGraphQLRequest(event)

            // execute
            val response = graphQLService.query(emptyMap(), graphRequest)

            // respond
            return ApiGatewayResponse(
                statusCode = 200,
                body = JsonUtil.toJson(response),
                headers = mapOf(
                    HEADER_ACCESS_CONTROL_ALLOW_ORIGIN to "*",
                    HEADER_REQUEST_ID to event.requestContext.requestId,
                    HEADER_RESPONSE_TIME to "${(System.currentTimeMillis() - start)}ms"
                )
            )

        }
        catch (e: Exception) {

            log.error("Request failed", e)

            val apiError = e.toApiError()

            return ApiGatewayResponse(
                statusCode = apiError.status,
                body = JsonUtil.toJson(apiError),
                headers = mapOf(
                    HEADER_ACCESS_CONTROL_ALLOW_ORIGIN to "*",
                    HEADER_REQUEST_ID to ctx.awsRequestId
                )
            )
        }
        finally {
            MDC.clear()
        }
    }

    private fun resolveGraphQLRequest(event: APIGatewayProxyRequestEvent) : GraphQLRequest {

        log.info("Resolving GraphQL request [httpMethod={}]", event.httpMethod)

        when(event.httpMethod) {
            "POST" -> {
                return when (event.isApplicationJson()) {
                    true -> JsonUtil.fromJson(event.body)
                    else -> GraphQLRequest(event.body)
                }
            }
            else -> throw IllegalArgumentException("HTTP Method not supported ${event.httpMethod}")
        }
    }
}

fun APIGatewayProxyRequestEvent.isApplicationJson() : Boolean {

    val contentType = this.headers["content-type"] ?: this.headers["Content-Type"] ?: this.headers["CONTENT-TYPE"] ?: ""

    return contentType.startsWith("application/json")
}
