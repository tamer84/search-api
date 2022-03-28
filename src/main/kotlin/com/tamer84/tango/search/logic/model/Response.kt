package com.tamer84.tango.search.logic.model

import com.fasterxml.jackson.annotation.JsonInclude

/*###########################################
 *           R E S P O N S E
 ############################################*/
data class Metadata(val total: Int, val limit: Int = 1, val offset: Int = 0)

/*###########################################
 *           R E S P O N S E
 ############################################*/
data class OverallResponse(val total: Int = 0)
data class OverviewResponse(val overall: OverallResponse,
                            val counts: Map<String,Int> = emptyMap(),
                            val stats: Map<String,Any> = emptyMap(),
                            val terms: Map<String,Any> = emptyMap())
/**
 * Logical categorization of metrics.  Corresponds to API fields so don't make them uppercase!
 */
@Suppress("EnumEntryName")
enum class OverviewCategory { counts, stats, terms }

data class StatsResult(val count: String, val min: String, val max: String, val avg: String)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class TermsResult(val value: String,
                       val count: Int = 0,
                       val localizations: Map<String, String>? = null,
                       val meta: Map<String,Any?>? = null)  : Comparable<TermsResult> {
    override fun compareTo(other: TermsResult): Int {
        if(this.value == "UNKNOWN" || this.value == "UNDEFINED") return 1
        return compareValuesBy(this, other, { it.localizations?.iterator()?.next()?.value }, { it.value }, { it.count })
    }
}

/**
 * TermsSubtermsResult holds term aggregation results that have additional dimensions (sub-terms).
 *
 * Why extend LinkedHashMap?
 *
 * First, LinkedHashMap preserves input order which makes the JSON look nicer.
 *
 * Second, by extending LinkedHashMap, this class can offer a combination of weak-typing (the variables declared in constructor).
 * and dynamic-typing via the map extension. The variables in the constructor are considered weakly-typed because they
 * can technically be overridden.
 *
 * The dynamic-typing allows this object to be re-used for different multi-dimensional combinations.  An example of a two
 * dimensional terms aggregation result is shown below where 'bodyId' is the outer dimension and 'modelSeriesId'
 * is the second dimension.  In this example, modelSeriesId is the 'dynamic field' that is added at runtime.
 *
 * <code>
    "bodyId": [{
        "value": "8",
        "count": 301,
        "localizations": { "fr": "Berline compacte"},
        "modelSeriesId": [{
            "value": "A",
            "count": 243
            },
            {
            "value": "B",
            "count": 57
            }
        ]
    }
 * </code>
 *
 */
data class TermsSubtermsResult(val value: String, val count: Int, val localizations: Map<String,Any>? = null) : LinkedHashMap<String, Any>() {
        init {
            this["value"] = value
            this["count"] = count
            localizations?.let { this["localizations"] = it }
        }
}

