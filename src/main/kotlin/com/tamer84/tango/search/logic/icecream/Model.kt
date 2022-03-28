package com.tamer84.tango.search.logic.icecream

import com.tamer84.tango.icecream.domain.icsi.model.IceCreamStockItem
import com.tamer84.tango.model.Market
import com.tamer84.tango.model.ProductType
import com.tamer84.tango.search.logic.model.*
import com.tamer84.tango.search.util.asFilterMap
import com.tamer84.tango.search.util.asSortFields
import com.tamer84.tango.search.logic.model.Metadata as Metadata1


data class IceCreamResponse(val metadata: Metadata1, val products: List<IceCreamStockReadModel>)

data class IceCreamStockReadModel(val createdDate: String?,
                                  val lastModifiedDate: String?,
                                  val updateCount: Long? = 1) : IceCreamStockItem()

data class IceCreamReq(val market: Market = Market.DE,
                       val limit: Int = 10,
                       val offset: Int = 0,
                       val sort: IceCreamSort = IceCreamSort(lastModifiedDate = SortDirection.DESC),
                       val where : IceCreamFilter? = IceCreamFilter(),
                       val fields: List<String> = emptyList())

fun IceCreamReq.toFilteredSearchReq() : FilteredSearchReq {
    return FilteredSearchReq(
        limit = limit,
        offset = offset,
        filters = where?.asFilterMap(ProductType.ICE_CREAM).orEmpty(),
        fields = fields,
        sortFields = sort.asSortFields(ProductType.ICE_CREAM)
    )
}

data class IceCreamFilter(val id: StringCondition? = null,
                          val availabilityStatus: StringCondition? = null,
                          val productCategoryCode: StringCondition? = null,
                          val productCode: StringCondition? = null,
                          val productType: StringCondition? = null,
                          val productPrice: FloatCondition? = null) : WhereFilter

data class IceCreamSort(val createdDate: SortDirection? = null,
                        val lastModifiedDate: SortDirection? = null,
                        val productPrice: SortDirection? = null) : SortInput
