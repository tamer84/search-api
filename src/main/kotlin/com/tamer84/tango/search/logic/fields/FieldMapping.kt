package com.tamer84.tango.search.logic.fields

import com.tamer84.tango.model.ProductType
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.slf4j.LoggerFactory

object FieldMapping {

    private val log = LoggerFactory.getLogger(FieldMapping::class.java)

    val iceCreamFields : Map<String, Field>

    init {
        log.info("Loading collect fields")
        val iceCreamConf = ConfigFactory.parseResources("_field_icecream.conf").resolve()
        iceCreamFields = iceCreamConf.extract<Fields>().fields.values.associateBy { it.alias }

    }

    fun hasField(productType: ProductType, alias: String) : Boolean {
        return when(productType) {
            ProductType.ICE_CREAM -> iceCreamFields.containsKey(alias)
            else -> false
        }
    }

    fun getField(productType: ProductType, alias: String) : Field {
        return when(productType) {
            ProductType.ICE_CREAM -> iceCreamFields[alias]
        } ?: throw IllegalStateException("$alias not defined")
    }

    fun getEsPath(productType: ProductType, alias: String) : String = getField(productType, alias).path
}

/**###########################################
 *    MODEL
############################################*/

data class Fields(val fields: Map<String, Field>)

/**
 * Represents a field
 *
 * @param alias the field's alias or short name
 * @param path the field's Elasticsearch search path (dot notated)
 * @param nestedPath the path to the nested field to where this field belongs (optional)
 * @param type the field's data type (defaults to <code>DataType.TEXT_KEYWORD</code>)
 */
data class Field(val alias: String,
                 val path: String,
                 val nestedPath: String? = null,
                 val type: DataType = DataType.TEXT_KEYWORD) {

    // This field is convenient for searching a JsonNode for the field in an aggregation response
    val jsonNodePath = when(nestedPath) {
        // when null, use the path
        null -> "/" + path.replace(".", "/")
        // otherwise, use the part of the path after the nested path
        else -> path.substringAfter(nestedPath).replace(".", "/")
    }
    fun isNested() = nestedPath != null
    fun keywordPath() = path + if(type == DataType.TEXT_KEYWORD) ".keyword" else ""
}

enum class DataType { BOOL,DATE,GEO,KEYWORD,NUMBER,TEXT_KEYWORD }
