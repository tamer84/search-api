package com.tamer84.tango.search.util

import com.tamer84.tango.model.ProductType
import com.tamer84.tango.search.logic.fields.Field
import com.tamer84.tango.search.logic.fields.FieldMapping
import com.tamer84.tango.search.logic.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.full.memberProperties


inline fun <reified T : WhereFilter> T.asFilterMap(productType: ProductType) : Map<Field, Condition> {
    return T::class.memberProperties.asSequence()
        .filterNot { it.getter.call(this) == null }
        .associate {
            FieldMapping.getField(productType, it.name) to it.getter.call(this) as Condition
        }
}
inline fun <reified T : SortInput> T.asSortFields(productType: ProductType) : List<SortField> {
    return T::class.memberProperties.asSequence()
        .filterNot { it.getter.call(this) == null }
        .map {
            SortField(
                field = FieldMapping.getEsPath(productType, it.name),
                direction = it.getter.call(this) as SortDirection)
        }.toList()
}

/**
 * Converts Double to String removing scientific notation
 *
 * @see BigDecimal.toPlainString
 */
fun Double.asPlainString(scale: Int = 2) : String {
    return BigDecimal(this).setScale(scale, RoundingMode.HALF_UP).toPlainString()
}

@JvmName("asStringsFloat")
fun List<Float>.asStrings() : List<String> = this.map { it.toString() }

@JvmName("asStringsInt")
fun List<Int>.asStrings() : List<String> = this.map { it.toString() }

/**
 * Converts list to a double quoted CSV
 */
fun List<String>.asDoubleQuotedCSV() = this.joinToString{""""$it""""}

/**
 * Converts list to double quoted CSV using the path
 */
fun List<Field>.asDoubleQuotedPathCSV() = this.joinToString { """"${it.path}"""" }

