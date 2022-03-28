package com.tamer84.tango.search.util


import com.tamer84.tango.search.config.EnvVar
import com.tamer84.tango.search.elasticsearch.ElasticsearchClient
import com.tamer84.tango.search.elasticsearch.QueryBuilder
import com.tamer84.tango.search.graphql.GraphQLRepo
import com.tamer84.tango.search.graphql.GraphQLService
import com.tamer84.tango.search.logic.icecream.IceCreamService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration

object IocContainer {

    private val okHttpClient = createOkHttpClient()

    private val elasticsearchClient = ElasticsearchClient(okHttpClient, EnvVar.elasticsearchUrl)
    private val queryService = QueryBuilder()

    private val iceCreamService = IceCreamService(elasticsearchClient, queryService)

    val graphQLRepo = GraphQLRepo(iceCreamService)

    val graphQLService = GraphQLService(graphQLRepo.graphQL)

    /**
     * Creates OkHttpClient that uses a socks proxy
     */
    fun createOkHttpClient() : OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor())
            .callTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
        if(EnvVar.LOCAL_MODE == "true") {
            val proxyAddress = InetSocketAddress("localhost", 1234)
            val proxy = Proxy(Proxy.Type.SOCKS, proxyAddress)
            builder.proxy(proxy)
        }
        return builder.build()
    }


    internal class LoggingInterceptor : Interceptor {

        companion object {
            private val log = LoggerFactory.getLogger("OkHttpClient")
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val req = chain.request()

            val t1 = System.nanoTime()
            log.info("${req.method} ${req.url}")

            val response = chain.proceed(req)

            if(! response.isSuccessful) {
                log.warn("Request headers: ${req.headers}")
            }

            val t2 = System.nanoTime()
            log.info("Response for ${req.url} in ${(t2 - t1) / 1e6} millis")

            return response
        }
    }
}
