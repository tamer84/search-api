package com.tamer84.tango.search.config


import com.tamer84.tango.model.Market
import com.tamer84.tango.model.ProductType
import org.slf4j.LoggerFactory

object EnvVar {

    private val log = LoggerFactory.getLogger(EnvVar::class.java)

    //====== Constants =======//

    const val MDC_REQUEST_ID = "reqId"

    val LOCAL_MODE : String            = getenvOrDefault("LOCAL_MODE", "false")
    val elasticsearchUrl : String      = getenvOrDefault("ELASTICSEARCH_URL", "https://vpc-tango-search-dev-3cvf4aqr34mky33vmmjenxqr7q.eu-central-1.es.amazonaws.com")

    fun index(productType: ProductType, market: Market) = "${market}".lowercase()
    fun iceCreamIndex(market: Market) = index(ProductType.ICE_CREAM, market)

    private fun getenv(key: String) : String? = System.getenv(key) ?: System.getProperty(key)
    private fun getenvOrDefault(key: String, default: String) : String {
        return getenv(key)  ?: default.also {
            log.warn("env_var [$key] not provided.  Using default value [$default]")
        }
    }
}
