package com.tamer84.tango.search.util

import java.io.BufferedReader
import java.io.InputStream


object IoUtil {

    fun resourceToString(filename: String) : String {
        val input = resourceToInputStream(filename)
        return input.bufferedReader().use(BufferedReader::readText)
    }

    fun resourceToInputStream(filename: String) : InputStream {
        val inputStream = this.javaClass.classLoader.getResourceAsStream(filename)
        checkNotNull(inputStream) { "File not found: [filename=$filename]"}
        return inputStream
    }
}
