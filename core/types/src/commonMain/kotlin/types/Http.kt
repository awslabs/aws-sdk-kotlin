package types

interface Headers : MutableMap<String, String>


interface Readable {

}

interface HttpMessage {
    val headers: Headers
    val body: Readable
}

interface QueryParameters : Map<String, List<String>>

interface Endpoint {
    val protocol: String
    val hostname: String
    val port: Int?
    val path: String
    val queryParameters: QueryParameters
}

interface HttpRequest : HttpMessage, Endpoint

interface HttpResponse : HttpMessage {
    val statusCode: Int
}