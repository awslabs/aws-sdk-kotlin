package types

import okio.Source

class Headers(private val delegate: MutableMap<String, String> = mutableMapOf()) :
    MutableMap<String, String> by delegate {
    fun copy(): Headers {
        return Headers(delegate.toMutableMap())
    }
}

class QueryParameters(private val delegate: MutableMap<String, MutableList<String>> = mutableMapOf()) :
    Map<String, List<String>> by delegate {
    fun copy(): QueryParameters {
        val newMap = mutableMapOf<String, MutableList<String>>()
        delegate.asSequence().map { it.key to it.value.toMutableList() }.toMap(newMap)
        return QueryParameters(newMap)
    }
}

sealed class Content
class StreamContent(val data: Source) : Content()
class StringContent(val data: String) : Content()

interface HttpMessage {
    val headers: Headers
    val body: Content?
}

interface Endpoint {
    val protocol: String
    val hostname: String
    val port: Int?
    val path: String
    val queryParameters: QueryParameters?
}

data class HttpRequest(
    override val headers: Headers,
    override val body: Content? = null,
    override val protocol: String,
    override val hostname: String,
    val method: String, // TODO: Worth making an enum?
    override val port: Int? = null,
    override val path: String,
    override val queryParameters: QueryParameters? = null
) : HttpMessage, Endpoint

interface HttpResponse : HttpMessage {
    val statusCode: Int
}