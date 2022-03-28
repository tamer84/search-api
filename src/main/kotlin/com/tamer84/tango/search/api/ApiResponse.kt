package com.tamer84.tango.search.api

data class ApiGatewayResponse(val statusCode: Int = 200,
                              val body: String = "",
                              val headers : Map<String, String> = emptyMap(),
                              @get:JvmName("getIsBase64Encoded") val isBase64Encoded : Boolean = false)

/**
 * Represents a comment Error structure
 */
data class ApiError(val status: Int = 500,
                    val message: String? = "Request failed",
                    val reqId : String? = "")
