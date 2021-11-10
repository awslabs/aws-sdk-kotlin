/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.http.HeadersBuilder
import aws.sdk.kotlin.crt.http.HttpRequestBodyStream
import aws.sdk.kotlin.crt.io.Protocol
import aws.sdk.kotlin.crt.io.Uri
import aws.sdk.kotlin.crt.io.UserInfo
import aws.sdk.kotlin.runtime.crt.ReadChannelBodyStream
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlin.coroutines.CoroutineContext

private const val CONTENT_LENGTH_HEADER: String = "Content-Length"

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

    val crtHeaders = HeadersBuilder()
    with(crtHeaders) {
        headers.forEach { key, values -> appendAll(key, values) }
    }

    val bodyLen = body.contentLength
    val contentLength = when {
        bodyLen != null -> if (bodyLen > 0) bodyLen.toString() else null
        else -> headers[CONTENT_LENGTH_HEADER]
    }
    contentLength?.let { crtHeaders.append(CONTENT_LENGTH_HEADER, it) }

    return aws.sdk.kotlin.crt.http.HttpRequest(method.name, url.encodedPath, crtHeaders.build(), bodyStream)
}
