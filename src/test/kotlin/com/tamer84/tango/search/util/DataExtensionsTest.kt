package com.tamer84.tango.search.util

import com.tamer84.tango.search.logic.fields.Field
import kotlin.test.Test
import kotlin.test.assertEquals

class DataExtensionsTest {

    @Test
    fun testDouble_asPlainString() {
        val double = 1.6217496E7
        assertEquals("16217496.00", double.asPlainString())
    }

    @Test
    fun testListExtension_AsDoubleQuotedCSV() {
        assertEquals(""""1", "2", "3"""", listOf("1", "2", "3").asDoubleQuotedCSV())
    }

    @Test
    fun testListFieldExtension_AsDoubleQuotedPathCSV() {
        val n = listOf(Field("alias1", "path1"), Field("alias2","path2"))
        assertEquals(""""path1", "path2"""", n.asDoubleQuotedPathCSV())
    }

    @Test
    fun testListFloat_asAsStrings() {
        assertEquals(listOf("1.5", "2.5", "3.8"), listOf(1.5f, 2.5f,3.8f).asStrings())
    }

    @Test
    fun testListInt_asAsStrings() {
        assertEquals(listOf("1", "2", "3"), listOf(1, 2, 3).asStrings())
    }

}
