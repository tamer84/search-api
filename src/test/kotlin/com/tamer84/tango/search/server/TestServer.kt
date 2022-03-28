package com.tamer84.tango.search.server

import com.tamer84.tango.search.api.ApiError
import com.tamer84.tango.search.api.NotAuthorizedException
import com.tamer84.tango.search.api.NotFoundException
import com.tamer84.tango.search.config.EnvVar
import io.javalin.Javalin
import io.javalin.core.compression.CompressionStrategy.Companion.GZIP
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

private val log = LoggerFactory.getLogger("TestServer")

/**
 * {"domain":"SEARCH","perms":[],"countries":[{"code":"FR","perms":["READ"],"companies":[{"id":"GC0003921","perms":["READ"]},{"id":"GC0011485","outlets":[{"id":"GS0014401","perms":["READ"]}]}]}]}
 */
fun main() {

    /*
    Setup Sock Proxy. Run the following in terminal before starting server
    > ssh -D 1234 -q -C -N ubuntu@i-08805c6ecfcdf2b37
     */
    System.setProperty("LOCAL_MODE", "true")
  //  System.setProperty("ELASTICSEARCH_URL", "http://localhost:1234")

    val app = Javalin.create { config ->
        config.compressionStrategy(GZIP)
        config.defaultContentType = "application/json"
        config.requestLogger { _, ms ->
            MDC.put("durationMs", ms.toInt().toString())
            log.info("REQUEST Completed")
            MDC.clear()
        }
        config.showJavalinBanner = false

    }.start(8001)

    app.before { MDC.put(EnvVar.MDC_REQUEST_ID, UUID.randomUUID().toString()) }

    mapExceptions(app)

    app.routes(apiEndpoints())
}


class ApiExceptionHandler(val status:Int = 500) : ExceptionHandler<Exception> {

    override fun handle(e: Exception, ctx: Context) {
        log.error("Request failed")
        e.printStackTrace()
        ctx.status(status).json(ApiError(status, e.message, MDC.get("traceId")))
    }

}

fun mapExceptions(app: Javalin) {

    app.exception(IllegalAccessException::class.java, ApiExceptionHandler(status = 401))
    app.exception(IllegalArgumentException::class.java, ApiExceptionHandler(status = 400))
    app.exception(IllegalStateException::class.java, ApiExceptionHandler(status = 500))
    app.exception(NotAuthorizedException::class.java, ApiExceptionHandler(status = 403))
    app.exception(NotFoundException::class.java, ApiExceptionHandler(status = 404))
    app.exception(NullPointerException::class.java, ApiExceptionHandler(status = 400))
    app.exception(Exception::class.java, ApiExceptionHandler(status = 500))

}
