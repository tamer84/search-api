package com.tamer84.tango.search

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.nhaarman.mockitokotlin2.mock
import com.tamer84.tango.search._tools.TestUtil
import com.tamer84.tango.search.graphql.GraphQLService
import com.tamer84.tango.search.util.JsonUtil
import kotlin.test.Test

class EventHandlerTest {

    val graphQLService = mock<GraphQLService>()

    val handler = EventHandler(graphQLService)

    @Test
    fun testInteractions() {
        val inputStream = TestUtil.resourceToInputStream("test-apigateway-api-auth.json")
        val apiGatewayRequest = JsonUtil.fromJson<APIGatewayProxyRequestEvent>(inputStream)
        val ctx = mock<Context>()

        handler.handleRequest(apiGatewayRequest, ctx)
    }

}
