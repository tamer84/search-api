package com.tamer84.tango.search.server

import com.tamer84.tango.search.graphql.GraphQLRequest
import com.tamer84.tango.search.util.IocContainer
import com.tamer84.tango.search.util.JsonUtil
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup


val graphQLService = IocContainer.graphQLService

fun apiEndpoints() : EndpointGroup {

    return EndpointGroup {

        path("/graphql") {
            post { ctx ->
                val req = when(ctx.contentType() == "application/json") {
                    true -> JsonUtil.fromJson(ctx.body())
                    else -> GraphQLRequest(ctx.body())
                }
                val data = graphQLService.query(emptyMap(), req)
                ctx.json(data)
            }
        }
    }
}

