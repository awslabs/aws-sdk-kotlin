/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.SdkDefaultIO
import aws.sdk.kotlin.crt.http.*
import aws.sdk.kotlin.crt.io.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.engine.HttpClientEngineBase
import software.aws.clientrt.http.engine.HttpClientEngineConfig
import software.aws.clientrt.http.engine.callContext
import software.aws.clientrt.http.request.HttpRequest
import software.aws.clientrt.http.response.HttpCall
import software.aws.clientrt.time.Instant

/**
 * [HttpClientEngine] based on the AWS Common Runtime HTTP client
 */
public class CrtHttpEngine(public val config: HttpClientEngineConfig) : HttpClientEngineBase("crt") {
    // FIXME - use the default TLS context when profile cred provider branch is merged
    private val tlsCtx = TlsContext(TlsContextOptions.defaultClient())

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
    private val mutex = Mutex()

    override suspend fun roundTrip(request: HttpRequest): HttpCall {
        val callContext = callContext()
        val manager = getManagerForUri(request.uri)
        val conn = manager.acquireConnection()

        try {
            val reqTime = Instant.now()
            val engineRequest = request.toCrtRequest(callContext)

            // LIFETIME: connection will be released back to the pool/manager when
            // the response completes OR on exception
            val respHandler = SdkStreamResponseHandler(conn)

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
        mutex.withLockNoSuspend {
            connManagers.forEach { entry -> entry.value.close() }
        }
        tlsCtx.close()
    }

    private suspend fun getManagerForUri(uri: Uri): HttpClientConnectionManager = mutex.withLock {
        connManagers.getOrPut(uri.host) {
            HttpClientConnectionManager(options.apply { this.uri = uri }.build())
        }
    }

    private fun <T> Mutex.withLockNoSuspend(block: () -> T): T {
        while (!tryLock()) { } // spin
        try {
            return block()
        } finally {
            unlock()
        }
    }
}
