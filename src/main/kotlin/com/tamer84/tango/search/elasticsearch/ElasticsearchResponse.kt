package com.tamer84.tango.search.elasticsearch

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.tamer84.tango.search.util.JsonUtil
import com.tamer84.tango.search.util.asPlainString


data class EsResponse(val took: Int,
                      val timed_out: Boolean,
                      val _shards: Map<String, Any> = emptyMap(),
                      val hits: EsHits) {
    fun totalHits() = hits.total.value
    inline fun <reified T> hitsAsType() : List<T> = hits.hits.map { h -> JsonUtil.convertObj(h._source) }
}

data class Total(val value: Int = 0, val relation: String = "eq")

data class EsHits(val total: Total = Total(),
                  val max_score: Float?,
                  val hits: List<EsHit> = emptyList())

data class EsHit(val _index: String,
                    val _type: String,
                    val _id: String,
                    val _score: Float?,
                    val found : Boolean = true,
                    val _source: Map<String,Any>) {
    inline fun <reified T> hitAsType(): T = JsonUtil.convertObj(_source)
}



data class EsMultiResponse(val took: Int, val responses: List<EsAggregationResponse> = emptyList())

data class EsAggregationResponse(val took: Int,
                                 val timed_out: Boolean,
                                 val _shards: Map<String, Any> = emptyMap(),
                                 @JsonProperty("hits") val hits: EsHits,
                                 val status: Int = 200,
                                 val aggregations: JsonNode) {
    fun totalHits() = hits.total.value
}

//  Aggregations
data class StatAggregation(val count: Long,
                           val min: Double,
                           val max: Double,
                           val avg: Double,
                           val min_as_string: String?,
                           val max_as_string: String?,
                           val avg_as_string: String?)  {
    fun count() = count.toString()

    // Removes scientific notation
    fun minIntegerText() = min.asPlainString(0)
    fun maxIntegerText() = max.asPlainString(0)
    fun avgDoubleText() = avg.asPlainString()

    // Accounts for Date fields
    fun min() = min_as_string ?: min.asPlainString()
    fun max() = max_as_string ?: max.asPlainString()
    fun avg() = avg_as_string ?: avg.asPlainString()
}



