package com.tamer84.tango.search.graphql

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.GraphQLContext
import org.slf4j.LoggerFactory
import java.util.*

data class GraphQLRequest(val query: String,
                          val operationName: String? = null,
                          val variables: Map<String,Any>? = emptyMap())

class GraphQLService(private val graphQL: GraphQL) {

    companion object {
        private val log = LoggerFactory.getLogger(GraphQLService::class.java)
    }

    fun query(ctx: Map<String,Any>, req: GraphQLRequest) : Any {

        log.info("Executing GraphQL query=[${req.query}]")

        val variables = req.variables ?: emptyMap()

        val input = ExecutionInput.Builder()
            .operationName(req.operationName)
            .query(req.query)
            .variables(variables)
            .locale(Locale.GERMANY)
            .graphQLContext(ctx)
            .build()


        // At this point in the code, the execution jumps to one of the GraphQLRepo data fetchers
        val res = graphQL.execute(input)

        return when(res.errors.isNullOrEmpty()) {
            true -> res.toSpecification()
            else -> res.toSpecification().also {
                log.error("Request failed ${res.errors}")
            }
        }
    }
}

fun createGraphQLContext(event: APIGatewayProxyRequestEvent) : GraphQLContext {
    return createGraphQLContext(event.path, event.headers, event.requestContext.authorizer)
}

fun createGraphQLContext(path: String, headers: Map<String,String>, requestCtx: Map<String, Any>?) : GraphQLContext {
    return GraphQLContext.Builder().of("key1", "key1-value").build()
}

