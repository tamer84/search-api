package com.tamer84.tango.search.elasticsearch

import com.tamer84.tango.search.util.JsonUtil
import net.logstash.logback.argument.StructuredArguments.raw
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Elasticsearch HttpClient

 * This class intentionally uses HttpClient to keep dependencies and the shaded JAR size to a minimum
 */
class ElasticsearchClient(private val client: OkHttpClient,
                          private val elasticsearchUrl: String) {

    companion object {
        private val log = LoggerFactory.getLogger(ElasticsearchClient::class.java)
        private const val APPLICATION_JSON = "application/json"
        val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
        val MEDIA_TYPE_NDJSON = "application/x-ndjson; charset=utf-8".toMediaType()
    }

    fun findDoc(index: String, id: String) : EsHit {

        val req = Request.Builder()
            .url("$elasticsearchUrl/$index/_doc/$id")
            .header("Accept", APPLICATION_JSON)
            .header("Content-Type", APPLICATION_JSON)
            .build()

        val json = sendRequest(req)

        return JsonUtil.fromJson(json)
    }

    /**
     * Sends multiple search queries to Elasticsearch which are then processed concurrently by ES.
     *
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-multi-search.html
     *
     * @param index the index
     * @param queries the JSON queries
     *
     * @return MultiSearchResponse
     */
    fun multiSearch(index: String, queries: List<String>) : EsMultiResponse {
        val json = post(
            "$elasticsearchUrl/$index/_msearch",
            QueryUtil.createMultiSearchQueries(queries),
            MEDIA_TYPE_NDJSON
        )
        return JsonUtil.fromJson<EsMultiResponse>(json).also {
            log.info("multiSearch complete [queryExecMs={}]", it.took)
        }
    }

    /**
     * Sends search query to Elasticsearch for items.
     *
     * @param index the index
     * @param query the JSON query
     * @return Response
     */
    fun search(index: String, query: String) : EsResponse {
        val json = post("$elasticsearchUrl/$index/_search", query)
        return JsonUtil.fromJson(json)
    }

    private fun post(url : String, json: String, mediaType: MediaType = MEDIA_TYPE_JSON) : String {

        log.info("Search: $url", raw("esQuery", json))

        val req = Request.Builder()
            .url(url)
            .header("Accept", APPLICATION_JSON)
            .header("Content-Type", mediaType.toString())
            .post(json.toRequestBody(mediaType))
            .build()

        return sendRequest(req)
    }

    private fun sendRequest(req: Request) : String {

        try {
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful)
                    return resp.body!!.string()
                throw IOException("Search failed").also {
                    log.error("Search failed [${resp.code}:${resp.message}]", raw("esResponse", resp.body?.string()))
                }
            }
        } catch (io: IOException) {
            throw IOException("CONNECTION FAILURE - ( method=[${req.method}], url=[${req.url}] )", io)
        }
    }
}
