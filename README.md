# Search GraphQL API

Product GraphQL API

## Tech Stack

* [Kotlin 1.5](https://kotlinlang.org/docs/reference/)
* AWS Lambda
* [GraphQL](https://www.graphql-java.com/)
* [Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/index.html)
* [HOCON](https://github.com/lightbend/config)

## Run Instructions (Local HTTP API)


### Steps

* Start Proxy

> ssh -D 1234 -q -C -N ubuntu@i-08805c6ecfcdf2b37

* Start TestServer

This test server is setup because it's a lot easier and more efficient to test the API via local server rather
than via the Lambda. 

* Send Requests 
* 
Send API requests (some options below):
    * [GraphiQL](https://www.electronjs.org/apps/graphiql)
    * Postman

