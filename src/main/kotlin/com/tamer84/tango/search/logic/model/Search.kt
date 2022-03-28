package com.tamer84.tango.search.logic.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.tamer84.tango.search.logic.fields.Field

/*###########################################
 *       S E A R C H   R E Q U E S T S
 ############################################*/
interface WhereFilter

data class FilteredSearchReq(val limit: Int = 10,
                             val offset: Int = 0,
                             val sortFields: List<SortField> = listOf(SortField("lastModifiedDate", SortDirection.DESC)),
                             val filters: Map<Field, Condition> = emptyMap(),
                             val fields: List<String> = emptyList()) {
    init {
        require(limit in 1..50) { "limit must be between 1-50" }
        require(offset > -1) { "offset must be 0 or greater"}
        require(sortFields.size < 3) { "a maximum of 2 sort fields are allowed"}
    }
}


/*###########################################
 *       S O R T I N G
 ############################################*/
interface SortInput
data class SortField(val field: String, val direction: SortDirection)
enum class SortDirection { @JsonProperty("asc") ASC, @JsonProperty("desc") DESC }

/*###########################################
 *       C O N D I T I O N S
 ############################################*/
interface Condition
data class BoolCondition(val eq: Boolean?, val neq: Boolean?) : Condition
data class DateCondition(val eq: String?, val neq: String?,
                         val gt: String? = null, val gte: String? = null,
                         val lt: String? = null, val lte: String? = null) : Condition {
    init {
        check(gt.isNullOrBlank() || gte.isNullOrBlank()) { "Only one option is allowed: [ gt or gte ]"}
        check(lt.isNullOrBlank() || lte.isNullOrBlank()) { "Only one option is allowed: [ lt or lte ]"}
    }

    @Suppress("UNCHECKED_CAST")
    val rangeRequest = mapOf("gt" to gt, "gte" to gte, "lt" to lt, "lte" to lte).filterNot { it.value == null } as Map<String,String>
}
data class FloatCondition(val eq: Float?, val neq: Float?,
                          val gt: Float? = null, val gte: Float? = null,
                          val lt: Float? = null, val lte: Float? = null,
                          val `in`: List<Float>?, val nin: List<Float>?) : Condition {
    init {
        check(gt==null || gte== null) { "Only one option is allowed: [ gt or gte ]"}
        check(lt==null || lte== null) { "Only one option is allowed: [ lt or lte ]"}
    }

    @Suppress("UNCHECKED_CAST")
    val rangeRequest = mapOf("gt" to gt, "gte" to gte, "lt" to lt, "lte" to lte).filterNot { it.value == null } as Map<String,Float>
}
data class IntCondition(val eq: Int? = null, val neq: Int? = null,
                        val gt: Int? = null, val gte: Int? = null,
                        val lt: Int? = null, val lte: Int? = null,
                        val `in`: List<Int>? = null, val nin: List<Int>? = null) : Condition {
    init {
        check(gt==null || gte== null) { "Only one option is allowed: [ gt or gte ]"}
        check(lt==null || lte== null) { "Only one option is allowed: [ lt or lte ]"}
    }

    @Suppress("UNCHECKED_CAST")
    val rangeRequest = mapOf("gt" to gt, "gte" to gte, "lt" to lt, "lte" to lte).filterNot { it.value == null } as Map<String,Int>
}
data class StringCondition(val eq: String?, val neq: String?,
                           val `in`: List<String>?, val nin: List<String>?) : Condition

data class PostalCodeCondition(val eq: PostalCodeLocation?, val neq: String?,
                               val `in`: List<String>?, val nin: List<String>?) : Condition

data class PostalCodeLocation(val postalCode: String, val radius: Int = 0) : Condition



