package com.tamer84.tango.search.logic.icecream

import com.tamer84.tango.model.Market
import com.tamer84.tango.search.api.NotFoundException
import com.tamer84.tango.search.config.EnvVar.iceCreamIndex
import com.tamer84.tango.search.elasticsearch.ElasticsearchClient
import com.tamer84.tango.search.elasticsearch.QueryBuilder
import com.tamer84.tango.search.logic.model.Metadata as Metadata1

class IceCreamService(private val client: ElasticsearchClient, private val queryBuilder: QueryBuilder) {

    /**
     * Find product by ID
     */
    fun findProduct(market: Market, productId: String) : IceCreamStockReadModel {

        val response = client.findDoc(iceCreamIndex(market), productId)

        if(response.found) {
            return response.hitAsType()
        }
        throw NotFoundException("Product not found [market=${market}, productId=$productId]")
    }

    fun search(req: IceCreamReq) : IceCreamResponse {

        val query = queryBuilder.buildSearchQuery(req.toFilteredSearchReq())

        val resp = client.search(iceCreamIndex(req.market), query)

        return IceCreamResponse(
            metadata = Metadata1(resp.totalHits(), req.limit, req.offset),
            products = resp.hitsAsType()
        )
    }
}
