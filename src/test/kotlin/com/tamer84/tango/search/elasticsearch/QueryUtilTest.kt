package com.tamer84.tango.search.elasticsearch

import org.junit.Assert.assertEquals
import kotlin.test.Test

class QueryUtilTest {

    ///////// Base Query Tests /////////

    @Test
    fun testCreateQuery() {
        val q = QueryUtil.createQuery("q", listOf("f", "f2"))
        assertEquals("""{ "track_total_hits": true, "query": { q }, "_source": { "includes": ["f", "f2"], "excludes": [] } }""", q)
    }

    @Test
    fun testConvertToMulti() {
        val queries = listOf("""
            { 
               "query": {     "match_all": {    } }
             }
        """.trimIndent(), """
            { 
               "query":       { "match_any": {} }
             }
        """.trimIndent())
        val multi = QueryUtil.createMultiSearchQueries(queries)

        val expected = """
{}
{ "query": { "match_all": { } } }
{}
{ "query": { "match_any": {} } }

""".trimIndent()
        assertEquals(expected, multi)
    }

    /////////////////  Other Queries  ////////////

    @Test
    fun testBoolFilters() {
        val r = QueryUtil.boolFilters(listOf("filter1", "filter2"))
        assertEquals(""""bool": { "filter": [ filter1, filter2 ] }""", r)
    }

    @Test
    fun testIds() {
        val q = QueryUtil.idsQuery(listOf("1","2"))
        assertEquals(""""ids" : { "values" : ["1", "2"] }""", q)
    }

    @Test
    fun testMatchAll() = assertEquals(""""match_all":{}""", QueryUtil.matchAll())

    ////////  range  /////////
    @Test
    fun testRangeQuery() {
        val r = QueryUtil.rangeQuery("field1", mapOf("gt" to "5", "lt" to "6"))
        assertEquals("""{ "range": { "field1": { "gt": "5", "lt": "6" } } }""", r)
    }

    /////////  Terms  ////////
    @Test
    fun testMustNotTermFilter() {
        val r = QueryUtil.mustNotTermFilter("field1", "value1")
        assertEquals("""{ "bool": { "must_not": { "term": { "field1": "value1" } } } }""", r)
    }

    @Test
    fun testMustNotTermsFilter() {
        val r = QueryUtil.mustNotTermsFilter("field1", listOf("value1", "value2"))
        assertEquals("""{ "bool": { "must_not": { "terms": { "field1": ["value1", "value2"] } } } }""", r)
    }

    @Test
    fun testShouldTermsFilter() {
        val r = QueryUtil.shouldTermsFilter(
            listOf(Pair("field1", "value1"), Pair("field2", "value1"))
        )
        assertEquals("""{ "bool" : { "should" : [{ "term": { "field1": "value1" } }, { "term": { "field2": "value1" } }], "minimum_should_match" : 1 } }""", r)
    }

    @Test
    fun testTermQuery() {
        val r = QueryUtil.termQuery("field1", "value1")
        assertEquals("""{ "term": { "field1": "value1" } }""", r)
    }

    @Test
    fun testTermsQuery() {
        val r = QueryUtil.termsQuery("field1", listOf("value1", "value2"))
        assertEquals("""{ "terms": { "field1": ["value1", "value2"] } }""", r)
    }
}
