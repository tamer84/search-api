package com.tamer84.tango.search._tools


import java.io.InputStream
import java.nio.charset.Charset
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TestUtil {

    fun resourceToInputStream(fileName: String) : InputStream {
        val input = this.javaClass.classLoader.getResourceAsStream(fileName)
        requireNotNull(input) { "file was not found or could not be read [$fileName]"}
        return input
    }

    fun resourceToString(fileName: String) : String {
        return resourceToInputStream(fileName).readBytes().toString(Charset.defaultCharset())
    }

    fun nowIso8601Utc() = ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT )

}
