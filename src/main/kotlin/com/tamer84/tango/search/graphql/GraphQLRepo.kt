package com.tamer84.tango.search.graphql

import com.tamer84.tango.icecream.domain.icsi.model.IceCreamStockItem
import graphql.GraphQL
import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.scalars.ExtendedScalars
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import com.tamer84.tango.model.Market
import com.tamer84.tango.search.logic.icecream.*
import com.tamer84.tango.search.util.IoUtil
import com.tamer84.tango.search.util.JsonUtil
import org.slf4j.LoggerFactory
import java.util.*

// extract top-level fields from graphQL selection so that we can reduce ElasticSearch response size
fun DataFetchingEnvironment.extractFields() : List<String> = this.selectionSet.fields
    .asSequence()
    .filter { it.selectionSet.fields.isEmpty()}
    // __typename is extra data added by some graphql clients
    .filterNot { it.name == "__typename"}
    .map { it.qualifiedName.replace("/",".") }.toList()

fun DataFetchingEnvironment.extractFields(prefix: String) : List<String> {
    return this.selectionSet.getFields("$prefix/**").asSequence()
        .filter { it.selectionSet.fields.isEmpty()}
        // __typename is extra data added by some graphql clients
        .filterNot { it.name == "__typename"}
        .map {
            it.qualifiedName.removePrefix("$prefix/").replace("/",".")
        }.toList()
}

fun DataFetchingEnvironment.containsObjectType(simpleTypeName: String) : Boolean {

    return this.selectionSet.fields.any {
        it.objectTypeNames.any { type ->
            type == simpleTypeName
        }
    }
}

class GraphQLRepo(private val iceCreamService: IceCreamService) {

    companion object {
        private val log = LoggerFactory.getLogger(GraphQLRepo::class.java)

        const val SCHEMA_FILE = "schema.graphql"
        const val QUERY = "Query"
    }

    /*###########################################
     *            Collection
     ############################################*/
    private val iceCreamFetcher: DataFetcher<IceCreamStockItem> = DataFetcher { env ->
        val id = env.getArgument<String>("id") ?: throw IllegalArgumentException("id is required")
        val market = env.getArgument<String>("market") ?: throw IllegalArgumentException("market is required")
        iceCreamService.findProduct(Market.valueOf(market), id)
    }
    private val iceCreamsFetcher: DataFetcher<IceCreamResponse> = DataFetcher { env ->
        val arg = env.getArgument<Any>("req") ?: throw IllegalArgumentException("req is required")
        val req = JsonUtil.convertObj<IceCreamReq>(arg)
        val fields = env.extractFields("products")
        iceCreamService.search(req.copy(fields = fields))
    }



    /*###########################################
     *           Q U E R I E S
     ############################################*/
    private val queries : Map<String, DataFetcher<*>> = mapOf(
        "iceCream"            to iceCreamFetcher,
        "iceCreams"           to iceCreamsFetcher
    )

    /*###########################################
     *           R U N T I M E
     ############################################*/
    private val runtimeWiring: RuntimeWiring = RuntimeWiring.newRuntimeWiring().apply {
        scalar(ExtendedScalars.Json)
        scalar(ExtendedScalars.Object)
        scalar(ExtendedScalars.Url)
        type(QUERY) { it.dataFetchers(queries) }
    }.build()


    val graphQL: GraphQL

    init {
        log.info("Initializing graphQL schema")
        val schema : String = IoUtil.resourceToString(SCHEMA_FILE)
        val typeDefinitionRegistry: TypeDefinitionRegistry = SchemaParser().parse(schema)
        val graphQLSchema: GraphQLSchema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
        graphQL = GraphQL.newGraphQL(graphQLSchema)
            .instrumentation(
                // this provides some basic protection against impolitely complex queries.  more should be investigated here
                ChainedInstrumentation(
                    MaxQueryComplexityInstrumentation(200),
                    MaxQueryDepthInstrumentation(20)
                )
            )
            .queryExecutionStrategy(AsyncExecutionStrategy(CustomExceptionHandler()))
            .build()
    }

    fun registeredQueries() = queries.keys
}
