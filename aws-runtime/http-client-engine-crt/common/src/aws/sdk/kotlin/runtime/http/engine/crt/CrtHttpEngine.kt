/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.http.*
import aws.sdk.kotlin.crt.io.*
import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.runtime.crt.SdkDefaultIO
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.callContext
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpCall
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.ExperimentalTime

internal const val DEFAULT_WINDOW_SIZE_BYTES: Int = 16 * 1024

/**
 * [HttpClientEngine] based on the AWS Common Runtime HTTP client
 */
@OptIn(ExperimentalTime::class)
public class CrtHttpEngine(public val config: CrtHttpEngineConfig) : HttpClientEngineBase("crt") {
    public constructor() : this(CrtHttpEngineConfig.Default)

    public companion object {
        public operator fun invoke(block: CrtHttpEngineConfig.Builder.() -> Unit): CrtHttpEngine = CrtHttpEngine(CrtHttpEngineConfig.Builder().apply(block).build())
    }
    private val logger = Logger.getLogger<CrtHttpEngine>()

    private val customTlsContext: TlsContext? = if (config.alpn.isNotEmpty() && config.tlsContext == null) {
        val options = TlsContextOptionsBuilder().apply {
            verifyPeer = true
            alpn = config.alpn.joinToString(separator = ";") { it.protocolId }
        }.build()
        TlsContext(options)
    } else {
        null
    }

    init {
        if (config.socketReadTimeout != CrtHttpEngineConfig.Default.socketReadTimeout) {
            logger.warn { "CrtHttpEngine does not support socketReadTimeout(${config.socketReadTimeout}); ignoring" }
        }
        if (config.socketWriteTimeout != CrtHttpEngineConfig.Default.socketWriteTimeout) {
            logger.warn { "CrtHttpEngine does not support socketWriteTimeout(${config.socketWriteTimeout}); ignoring" }
        }
    }

    @OptIn(ExperimentalTime::class)
    private val options = HttpClientConnectionManagerOptionsBuilder().apply {
        clientBootstrap = config.clientBootstrap ?: SdkDefaultIO.ClientBootstrap
        tlsContext = customTlsContext ?: config.tlsContext ?: SdkDefaultIO.TlsContext
        manualWindowManagement = true
        socketOptions = SocketOptions(
            connectTimeoutMs = config.connectTimeout.inWholeMilliseconds.toInt()
        )
        initialWindowSize = config.initialWindowSizeBytes
        maxConnections = config.maxConnections.toInt()
        maxConnectionIdleMs = config.connectionIdleTimeout.inWholeMilliseconds
    }

    // connection managers are per host
    private val connManagers = mutableMapOf<String, HttpClientConnectionManager>()
    private val mutex = Mutex()

    @OptIn(ExperimentalTime::class)
    override suspend fun roundTrip(request: HttpRequest): HttpCall {
        val callContext = callContext()
        val manager = getManagerForUri(request.uri)
        val conn = withTimeoutOrNull(config.connectionAcquireTimeout) {
            manager.acquireConnection()
        } ?: throw ClientException("timed out waiting for an HTTP connection to be acquired from the pool")

        try {
            val reqTime = Instant.now()
            val engineRequest = request.toCrtRequest(callContext)

            // LIFETIME: connection will be released back to the pool/manager when
            // the response completes OR on exception
            val respHandler = SdkStreamResponseHandler(conn)
            callContext.job.invokeOnCompletion {
                // ensures the stream is driven to completion regardless of what the downstream consumer does
                respHandler.complete()
            }

            val stream = conn.makeRequest(engineRequest, respHandler)
            stream.activate()

            val resp = respHandler.waitForResponse()

            return HttpCall(request, resp, reqTime, Instant.now(), callContext)
        } catch (ex: Exception) {
            try {
                manager.releaseConnection(conn)
            } catch (ex2: Exception) {
                ex.addSuppressed(ex2)
            }
            throw ex
        }
    }

    override fun shutdown() {
        // close all resources
        // SAFETY: shutdown is only invoked once AND only after all requests have completed and no more are coming
        connManagers.forEach { entry -> entry.value.close() }
        customTlsContext?.close()
    }

    private suspend fun getManagerForUri(uri: Uri): HttpClientConnectionManager = mutex.withLock {
        connManagers.getOrPut(uri.authority) {
            HttpClientConnectionManager(options.apply { this.uri = uri }.build())
        }
    }
}
