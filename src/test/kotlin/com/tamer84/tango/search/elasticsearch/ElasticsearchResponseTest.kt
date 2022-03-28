package com.tamer84.tango.search.elasticsearch

import com.tamer84.tango.search._tools.TestUtil
import com.tamer84.tango.search.util.JsonUtil
import org.junit.Assert
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ElasticsearchResponseTest {


    @Ignore
    @Test
    fun testEsResponse_deserialize() {

        val input = TestUtil.resourceToInputStream("fixtures/es/search-response.json")

        val response = JsonUtil.fromJson<EsResponse>(input)

        Assert.assertEquals(10, response.hits.hits.size)
    }

    @Test
    fun testStatAggregation() {
        val sa = StatAggregation(
            count = 1000,
            min = 48094.29,
            max = 323412423.01,
            avg = 48094.29187192118,
            avg_as_string = null,
            max_as_string = null,
            min_as_string = null
        )

        assertEquals("48094.29", sa.min())
        assertEquals("323412423.01", sa.max())
        assertEquals("48094.29", sa.avg())
    }
}
