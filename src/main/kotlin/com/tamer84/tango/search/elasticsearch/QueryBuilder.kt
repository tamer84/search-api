package com.tamer84.tango.search.elasticsearch

import com.tamer84.tango.search.logic.fields.Field
import com.tamer84.tango.search.logic.model.*
import com.tamer84.tango.search.util.asStrings
import org.slf4j.LoggerFactory

class QueryBuilder {

    companion object {
        private val log = LoggerFactory.getLogger(QueryBuilder::class.java)
    }



    /**
     * Builds a filtered count query
     *
     * @param filters the filters to apply
     */
    fun buildFilteredCountQuery(filters: Map<Field, Condition>) : String {
        val query = FilteredQueryBuilder(filters).build()
        return QueryUtil.createCountQuery(query)
    }

    /**
     * Builds a search query
     *
     * @param req the search request
     */
    fun buildSearchQuery(req: FilteredSearchReq) : String {

        val query = FilteredQueryBuilder(req.filters).build()

        return QueryUtil.createQuery(
            from = req.offset,
            size = req.limit,
            query = query,
            includes = req.fields,
            sortFields = req.sortFields
        )
    }


    /**
     * Encapsulates building filters queries for a given whereFilter.
     *
     * This is an inner class so that filters and nested filters state is encapsulated during each request
     * and so that this class has access to the geoService
     */
    private inner class FilteredQueryBuilder(private val whereFilter: Map<Field, Condition> = emptyMap(),
                                             excludeField: Field? = null) {

        /**
         * List of filter queries
         */
        private val filters = mutableListOf<String>()

        /**
         * Map of nested filter queries where key is the nestedPath
         */
        private val nestedFilters = mutableMapOf<String, MutableList<String>>()

        /**
         * Fields to exclude from filtering even if they are part of WhereFilter
         */
        private val filterExclusions = if(excludeField == null) emptySet<Field>() else setOf(excludeField.alias)

        /**
         * Creates the Boolean Filter query
         */
        fun build(): String {

            whereFilter.asSequence().filterNot { filterExclusions.contains(it.key) }.forEach { (field, condition) ->

                val filter = toFilterQuery(field, condition)

                when (field.isNested()) {
                    true -> filter?.let { nestedFilters.getOrPut(field.nestedPath!!) { mutableListOf() }.add(it) }
                    else -> filter?.let { filters.add(it) }
                }
            }

            val allFilters = filters.union(
                QueryUtil.nestedBoolFilters(nestedFilters)
            ).toList()

            return when (allFilters.isEmpty()) {
                true -> QueryUtil.matchAll()
                else -> QueryUtil.boolFilters(allFilters)
            }
        }

        private fun toFilterQuery(field: Field, condition: Condition): String? {

            val path = field.keywordPath()

            return when (condition) {
                is BoolCondition -> {
                    condition.eq?.let { return QueryUtil.termQuery(path, it.toString()) }
                    condition.neq?.let { return QueryUtil.mustNotTermFilter(path, it.toString()) }
                }
                is DateCondition -> {
                    condition.eq?.let { return QueryUtil.termQuery(path, it) }
                    condition.neq?.let { return QueryUtil.mustNotTermFilter(path, it) }
                    if (condition.rangeRequest.isNotEmpty())
                        return QueryUtil.rangeQuery(path, condition.rangeRequest)
                    else null
                }
                is FloatCondition -> {
                    condition.eq?.let { return QueryUtil.termQuery(path, it.toString()) }
                    condition.neq?.let { return QueryUtil.mustNotTermFilter(path, it.toString()) }
                    condition.`in`?.let { return QueryUtil.termsQuery(path, it.asStrings()) }
                    condition.nin?.let { return QueryUtil.mustNotTermsFilter(path, it.asStrings()) }
                    if (condition.rangeRequest.isNotEmpty())
                        QueryUtil.rangeQuery(path, condition.rangeRequest)
                    else null
                }
                is IntCondition -> {
                    condition.eq?.let { return QueryUtil.termQuery(path, it.toString()) }
                    condition.neq?.let { return QueryUtil.mustNotTermFilter(path, it.toString()) }
                    condition.`in`?.let { return QueryUtil.termsQuery(path, it.asStrings()) }
                    condition.nin?.let { return QueryUtil.mustNotTermsFilter(path, it.asStrings()) }
                    if (condition.rangeRequest.isNotEmpty())
                        QueryUtil.rangeQuery(path, condition.rangeRequest)
                    else null
                }
                is StringCondition -> {
                    condition.eq?.let { return QueryUtil.termQuery(path, it) }
                    condition.neq?.let { return QueryUtil.mustNotTermFilter(path, it) }
                    condition.`in`?.let { return QueryUtil.termsQuery(path, it) }
                    condition.nin?.let { return QueryUtil.mustNotTermsFilter(path, it) }
                }
                else -> {
                    log.warn("Condition unrecognized: $condition")
                    null
                }
            }
        }
    }

}
