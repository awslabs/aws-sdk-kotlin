/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.http.Headers
import aws.sdk.kotlin.crt.http.HttpRequestBodyStream
import aws.sdk.kotlin.crt.io.Protocol
import aws.sdk.kotlin.crt.io.Uri
import aws.sdk.kotlin.crt.io.UserInfo
import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.request.HttpRequest
import kotlin.coroutines.CoroutineContext

internal val HttpRequest.uri: Uri
    get() {
        val sdkUrl = this.url
        return Uri.build {
            scheme = Protocol.createOrDefault(sdkUrl.scheme.protocolName)
            host = sdkUrl.host
            port = sdkUrl.port
            userInfo = sdkUrl.userInfo?.let { UserInfo(it.username, it.password) }
            // the rest is part of each individual request, manager only needs the host info
        }
    }

internal fun HttpRequest.toCrtRequest(callContext: CoroutineContext): aws.sdk.kotlin.crt.http.HttpRequest {
    val body = this.body
    val bodyStream = when (body) {
        is HttpBody.Streaming -> ReadChannelBodyStream(body.readFrom(), callContext)
        is HttpBody.Bytes -> HttpRequestBodyStream.fromByteArray(body.bytes())
        else -> null
    }

    return aws.sdk.kotlin.crt.http.HttpRequest(method.name, url.encodedPath, HttpHeadersCrt(headers), bodyStream)
}

internal class HttpHeadersCrt(private val sdkHeaders: software.aws.clientrt.http.Headers) : Headers {
    override fun contains(name: String): Boolean = sdkHeaders.contains(name)
    override fun entries(): Set<Map.Entry<String, List<String>>> = sdkHeaders.entries()
    override fun getAll(name: String): List<String>? = sdkHeaders.getAll(name)
    override fun isEmpty(): Boolean = sdkHeaders.isEmpty()
    override fun names(): Set<String> = sdkHeaders.names()
}
