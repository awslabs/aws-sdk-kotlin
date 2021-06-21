/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.crt

import aws.sdk.kotlin.crt.http.HttpRequestBodyStream
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.util.splitAsQueryParameters
import aws.sdk.kotlin.crt.http.Headers as HeadersCrt
import aws.sdk.kotlin.crt.http.HttpRequest as HttpRequestCrt

/**
 * Convert an [HttpRequestBuilder] into a CRT HttpRequest for the purpose of signing.
 */
@InternalSdkApi
public fun HttpRequestBuilder.toSignableCrtRequest(): HttpRequestCrt {
    // FIXME - this does not account for streaming bodies. The main use case for this conversion is for signing purposes
    // only. We need to special case file streams as being signable. Custom dynamic streams that implement
    // HttpBody.Streaming are not signable without consuming the stream and would need to go through
    // chunked signing or unsigned payload
    // see: https://github.com/awslabs/smithy-kotlin/issues/297
    val bodyStream = (body as? HttpBody.Bytes)?.let { HttpRequestBodyStream.fromByteArray(it.bytes()) }
    return HttpRequestCrt(method.name, url.encodedPath, HttpHeadersCrt(headers), bodyStream)
}

// proxy the smithy-client-rt version of Headers to CRT (which is based on our client-rt version in the first place)
private class HttpHeadersCrt(val headers: HeadersBuilder) : HeadersCrt {
    override fun contains(name: String): Boolean = headers.contains(name)
    override fun entries(): Set<Map.Entry<String, List<String>>> = headers.entries()
    override fun getAll(name: String): List<String>? = headers.getAll(name)
    override fun isEmpty(): Boolean = headers.isEmpty()
    override fun names(): Set<String> = headers.names()
}

/**
 * Update a request builder from a CRT HTTP request (primary use is updating a request builder after signing)
 */
@InternalSdkApi
public fun HttpRequestBuilder.update(crtRequest: HttpRequestCrt) {
    // overwrite with crt request values
    headers.clear()
    url.parameters.clear()

    crtRequest.headers.entries().forEach { entry ->
        headers.appendAll(entry.key, entry.value)
    }

    // uri - we overwrite because the values may have been double encoded during signing
    if (crtRequest.encodedPath.isNotBlank()) {
        url.path = crtRequest.path()
        crtRequest.queryParameters()?.let {
            url.parameters.appendAll(it)
        }
    }
}

/**
 * Get just the query parameters (if any)
 * @return the query parameters from the path or null if there weren't any
 */
@InternalSdkApi
public fun HttpRequestCrt.queryParameters(): QueryParameters? {
    val idx = encodedPath.indexOf("?")
    if (idx < 0 || idx + 1 > encodedPath.length) return null

    val fragmentIdx = encodedPath.indexOf("#", startIndex = idx)
    val rawQueryString = if (fragmentIdx > 0) encodedPath.substring(idx + 1, fragmentIdx) else encodedPath.substring(idx + 1)
    return rawQueryString.splitAsQueryParameters()
}

/**
 * Get just the encoded path sans any query or fragment
 * @return the URI path segment from the encoded path
 */
@InternalSdkApi
public fun HttpRequestCrt.path(): String {
    val idx = encodedPath.indexOf("?")
    return if (idx > 0) encodedPath.substring(0, idx) else encodedPath
}
