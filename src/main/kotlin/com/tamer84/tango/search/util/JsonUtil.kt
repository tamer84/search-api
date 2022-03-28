package com.tamer84.tango.search.util


import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream

// WARNING: DO NOT SET Include.NON_EMPTY.  This interferes with GraphQl schema (a known issue)
// details: https://stackoverflow.com/questions/55925747/graphql-java-error-introspection-result-missing-interfaces
val mapper: ObjectMapper = jacksonObjectMapper()
        .registerModules(Jdk8Module())
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

object JsonUtil {

    inline fun <reified T> convertObj(obj: Any) : T  = mapper.convertValue(obj)

    inline fun <reified T> fromJson(inst: InputStream) : T = mapper.readValue(inst)

    inline fun <reified T> fromJson(json : String) : T = mapper.readValue(json)

    fun toJson(any: Any) : String = mapper.writeValueAsString(any)

    fun toJsonPretty(any: Any) : String = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(any)
}

