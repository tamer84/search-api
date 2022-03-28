package com.tamer84.tango.search.elasticsearch

import com.tamer84.tango.search.logic.model.SortField
import com.tamer84.tango.search.util.asDoubleQuotedCSV
import org.apache.commons.lang3.StringUtils


/**
 * Utility for creating Elasticsearch queries
 */
object QueryUtil {

    /*###########################################
     *    CONSTANTS
     ############################################*/
    private const val AGGREGATION_TYPE = "type"
    const val AGGREGATION_TYPE_JSON_PATH = "/meta/$AGGREGATION_TYPE"

    private const val TERM_ID = "termId"
    const val TERM_ID_JSON_PATH = "/meta/$TERM_ID"

    const val METADATA = "metadata"

    /*###########################################
     *       BASE QUERIES
     ############################################*/
    fun createQuery(query: String, includes: List<String> = emptyList(), excludes : List<String> = emptyList()) : String {
        val includeCsv = includes.asDoubleQuotedCSV()
        val excludesCsv = excludes.asDoubleQuotedCSV()
        return """{ "track_total_hits": true, "query": { $query }, "_source": { "includes": [$includeCsv], "excludes": [$excludesCsv] } }"""
    }

    fun createQuery(from: Int = 0, size: Int = 10,
                    includes: List<String> = emptyList(),
                    excludes : List<String> = emptyList(),
                    query: String, sortFields: List<SortField> = emptyList()) : String {
        val includeCsv = includes.asDoubleQuotedCSV()
        val excludesCsv = excludes.asDoubleQuotedCSV()
        val sort = sortFields.joinToString { """{ "${it.field}": { "order": "${it.direction}" } }""" }
        return """{ 
            "track_total_hits": true, 
            "from": $from, "size": $size, 
            "query": { $query },
            "_source": { "includes": [$includeCsv], "excludes": [$excludesCsv] },
            "sort": [ $sort ] 
        }"""
    }


    fun createCountQuery(query: String) : String {
        return """{ "track_total_hits": true, "size": 0, "query": { $query } }"""
    }

    /**
     * Multi-search requests are new line separated and must end in a new line character.
     * Each query must appear on precisely one line.
     */
    fun createMultiSearchQueries(queries: List<String>) : String {
        return "{}\n"+ queries.joinToString("\n{}\n") {
            StringUtils.normalizeSpace(it.replace("[\\n\\t\\r]".toRegex(), ""))
        } + "\n"
    }

    /*###########################################
     *       B O O L   F I L T E R S
     ############################################*/
    fun boolFilters(filters: List<String>) : String {
        return """"bool": { "filter": [ ${filters.joinToString()} ] }"""
    }

    fun nestedBoolFilters(req: Map<String, List<String>>) : List<String> {
        return req.map { (nestedPath, queries) ->
            """{
                "nested": {
                    "path": "$nestedPath",
                    "query": {
                        "bool": { "filter": [${queries.joinToString()}] }
                    }
                }
            }""".trimIndent()
        }
    }

    /*###########################################
     *       IDs
     ############################################*/
    fun idsQuery(ids: List<String>) : String {
        val idsCsv = ids.asDoubleQuotedCSV()
        return """"ids" : { "values" : [$idsCsv] }"""
    }

    /*###########################################
     *       MATCH ALL
     ############################################*/
    fun matchAll() = """"match_all":{}"""

    /*###########################################
     *              R A N G E
     ############################################*/
    fun rangeQuery(field: String, req: Map<String,Any>) : String {
        if(req.isEmpty()) return ""
        val range = req.map { """"${it.key}": "${it.value}"""" }.joinToString()
        return """{ "range": { "$field": { $range } } }"""
    }


    /*###########################################
     *       T E R M  -  (filters + queries))
     ############################################*/

    //https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html
    fun mustNotTermFilter(field: String, value: String) : String {
        return """{ "bool": { "must_not": { "term": { "$field": "$value" } } } }"""
    }
    fun mustNotTermsFilter(field: String, values: List<String>) : String {
        val valuesCsv = values.asDoubleQuotedCSV()
        return """{ "bool": { "must_not": { "terms": { "$field": [$valuesCsv] } } } }"""
    }
    fun shouldTermsFilter(fieldValues: Map<String,List<String>>) : String {
        val termQueries = fieldValues.map { e -> termsQuery(e.key, e.value) }
        return """{ "bool" : { "should" : $termQueries, "minimum_should_match" : 1 } }"""
    }
    fun shouldTermsFilter(fieldValues: List<Pair<String,String>>) : String {
        val termQueries = fieldValues.map { termQuery(it.first, it.second) }
        return """{ "bool" : { "should" : $termQueries, "minimum_should_match" : 1 } }"""
    }
    fun termQuery(field: String, value: String) : String {
        return """{ "term": { "$field": "$value" } }"""
    }
    fun termsQuery(field: String, values: List<String>) : String {
        val quotedStr = values.joinToString{""""$it""""}
        return """{ "terms": { "$field": [$quotedStr] } }"""
    }

}

