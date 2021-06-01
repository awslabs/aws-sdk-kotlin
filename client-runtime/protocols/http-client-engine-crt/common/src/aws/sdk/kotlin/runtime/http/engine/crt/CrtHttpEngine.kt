/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.SdkDefaultIO
import aws.sdk.kotlin.crt.http.*
import aws.sdk.kotlin.crt.io.*
import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.request.HttpRequest
import software.aws.clientrt.http.response.HttpCall
import software.aws.clientrt.logging.Logger
import software.aws.clientrt.time.Instant
import kotlin.coroutines.*
import aws.sdk.kotlin.crt.http.HttpRequest as HttpRequestCrt

/**
 * [HttpClientEngine] based on the AWS Common Runtime HTTP client
 */
public class CrtHttpEngine : HttpClientEngine {
    // FIXME - use the default TLS context when profile cred provider branch is merged
    private val tlsCtx = TlsContext(TlsContextOptions.defaultClient())
    private val logger = Logger.getLogger<CrtHttpEngine>()

    private val options = HttpClientConnectionManagerOptionsBuilder().apply {
        clientBootstrap = SdkDefaultIO.ClientBootstrap
        tlsContext = tlsCtx
        manualWindowManagement = true
        socketOptions = SocketOptions()
        initialWindowSize = 16 * 1024
        // TODO - max connections/timeouts/etc
    }

    // connection managers are per host
    private val connManagers = mutableMapOf<String, HttpClientConnectionManager>()

    override suspend fun roundTrip(request: HttpRequest): HttpCall {

        val manager = getManagerForUri(request.uri)
        val conn = manager.acquireConnection()

        try {
            val reqTime = Instant.now()
            val engineRequest = request.toCrtRequest(coroutineContext)

            // LIFETIME: connection will be released back to the pool/manager when
            // the response completes
            val respHandler = SdkStreamResponseHandler(conn)

            val stream = conn.makeRequest(engineRequest, respHandler)
            stream.activate()

            val resp = respHandler.waitForResponse()

            return HttpCall(request, resp, reqTime, Instant.now())
        } catch (ex: Exception) {
            manager.releaseConnection(conn)
            throw ex
        }
    }

    override fun close() {
        // close all resources
        connManagers.forEach { entry -> entry.value.close() }
        tlsCtx.close()
    }

    private fun getManagerForUri(uri: Uri): HttpClientConnectionManager =
        connManagers.getOrPut(uri.host) {
            HttpClientConnectionManager(options.apply { this.uri = uri }.build())
        }
}

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

internal fun HttpRequest.toCrtRequest(callContext: CoroutineContext): HttpRequestCrt {
    val body = this.body
    val bodyStream = when (body) {
        is HttpBody.Streaming -> ReadChannelBodyStream(body.readFrom(), callContext)
        is HttpBody.Bytes -> HttpRequestBodyStream.fromByteArray(body.bytes())
        else -> null
    }

    return HttpRequestCrt(method.name, url.encodedPath, HttpHeadersCrt(headers), bodyStream)
}

internal class HttpHeadersCrt(private val sdkHeaders: software.aws.clientrt.http.Headers) : Headers {
    override fun contains(name: String): Boolean = sdkHeaders.contains(name)
    override fun entries(): Set<Map.Entry<String, List<String>>> = sdkHeaders.entries()
    override fun getAll(name: String): List<String>? = sdkHeaders.getAll(name)
    override fun isEmpty(): Boolean = sdkHeaders.isEmpty()
    override fun names(): Set<String> = sdkHeaders.names()
}
