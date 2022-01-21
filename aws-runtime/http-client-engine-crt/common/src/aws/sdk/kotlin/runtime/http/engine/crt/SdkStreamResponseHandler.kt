/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.CRT
import aws.sdk.kotlin.crt.http.*
import aws.sdk.kotlin.crt.io.Buffer
import aws.sdk.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.logging.Logger
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel

/**
 * Implements the CRT stream response interface which proxies the response from the CRT to the SDK
 * @param conn The HTTP connection used to make the request. Will be closed when the response handler completes
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SdkStreamResponseHandler(
    private val conn: HttpClientConnection
) : HttpStreamResponseHandler {
    // TODO - need to cancel the stream when the body is closed from the caller side early.
    // There is no great way to do that currently without either (1) closing the connection or (2) throwing an
    // exception from a callback such that AWS_OP_ERROR is returned. Wait for HttpStream to have explicit cancellation

    private val logger = Logger.getLogger<SdkStreamResponseHandler>()
    private val responseReady = Channel<HttpResponse>(1)
    private val headers = HeadersBuilder()

    private var sdkBody: BufferedReadChannel? = null

    private val lock = reentrantLock() // protects crtStream and cancelled state
    private var crtStream: HttpStream? = null
    // if the (coroutine) job is completed before the stream's onResponseComplete callback is
    // invoked (for any reason) we consider the stream "cancelled"
    private var cancelled = false

    private val Int.isMainHeadersBlock: Boolean
        get() = when (this) {
            HttpHeaderBlock.MAIN.blockType -> true
            else -> false
        }

    private var streamCompleted = false

    /**
     * Called by the response read channel as data is consumed
     * @param size the number of bytes consumed
     */
    private fun onDataConsumed(size: Int) {
        lock.withLock {
            crtStream?.incrementWindow(size)
        }
    }

    override fun onResponseHeaders(
        stream: HttpStream,
        responseStatusCode: Int,
        blockType: Int,
        nextHeaders: List<HttpHeader>?
    ) {
        if (!blockType.isMainHeadersBlock) return

        nextHeaders?.forEach {
            headers.append(it.name, it.value)
        }
    }

    private fun createHttpResponseBody(contentLength: Long): HttpBody {
        sdkBody = bufferedReadChannel(::onDataConsumed)
        return object : HttpBody.Streaming() {
            override val contentLength: Long = contentLength
            override fun readFrom(): SdkByteReadChannel {
                return sdkBody!!
            }
        }
    }

    // signal response ready and engine can proceed (all that is required is headers, body is consumed asynchronously)
    private fun signalResponse(stream: HttpStream) {
        // already signalled
        if (responseReady.isClosedForSend) return

        val transferEncoding = headers["Transfer-Encoding"]?.lowercase()
        val chunked = transferEncoding == "chunked"
        val contentLength = headers["Content-Length"]?.toLong() ?: 0
        val status = HttpStatusCode.fromValue(stream.responseStatusCode)

        val hasBody = (contentLength > 0 || chunked) &&
            (status !in listOf(HttpStatusCode.NotModified, HttpStatusCode.NoContent)) &&
            !status.isInformational()

        val body = when (hasBody) {
            false -> HttpBody.Empty
            true -> createHttpResponseBody(contentLength)
        }

        val resp = HttpResponse(
            status,
            headers.build(),
            body
        )

        val result = responseReady.trySend(resp)
        check(result.isSuccess) { "signalling response failed, result was: ${result.exceptionOrNull()}" }
        responseReady.close()
    }

    override fun onResponseHeadersDone(stream: HttpStream, blockType: Int) {
        if (!blockType.isMainHeadersBlock) return
        signalResponse(stream)
    }

    override fun onResponseBody(stream: HttpStream, bodyBytesIn: Buffer): Int {
        val isCancelled = lock.withLock {
            crtStream = stream
            cancelled
        }

        // short circuit, stop buffering data and discard remaining incoming bytes
        if (isCancelled) return bodyBytesIn.len

        // we should have created a response channel if we expected a body
        val sdkRespChan = checkNotNull(sdkBody) { "unexpected response body" }
        sdkRespChan.write(bodyBytesIn)

        // explicit window management is done in BufferedReadChannel which calls `onDataConsumed`
        // as data is read from the channel
        return 0
    }

    override fun onResponseComplete(stream: HttpStream, errorCode: Int) {
        // stream is only valid until the end of this callback, ensure any further data being read downstream
        // doesn't call incrementWindow on a resource that has been free'd
        lock.withLock {
            crtStream = null
            streamCompleted = true
        }

        // close the body channel
        if (errorCode != 0) {
            val errorDescription = CRT.errorString(errorCode)
            val ex = ClientException("CrtHttpEngine::response failed: ec=$errorCode; description=$errorDescription")
            responseReady.close(ex)
            sdkBody?.cancel(ex)
        } else {
            // closing the channel to indicate no more data will be sent
            sdkBody?.close()
            // ensure a response was signalled (will close the channel on it's own if it wasn't already sent)
            signalResponse(stream)
        }
    }

    internal suspend fun waitForResponse(): HttpResponse {
        return responseReady.receive()
    }

    /**
     * Invoked only after the consumer is finished with the response and it is safe to cleanup resources
     */
    internal fun complete() {
        // We have no way of cancelling the stream, we have to drive it to exhaustion OR close the connection.
        // At this point we know it's safe to release resources so if the stream hasn't completed yet
        // we forcefully shutdown the connection. This can happen when the stream's window is full and it's waiting
        // on the window to be incremented to proceed (i.e. the user didn't consume the stream for whatever reason
        // and more data is pending arrival). It can also happen if the coroutine for this request is cancelled
        // before onResponseComplete fires.
        lock.withLock {
            val forceClose = !streamCompleted

            if (forceClose) {
                logger.debug { "stream did not complete before job, forcing connection shutdown! handler=$this; conn=$conn; stream=$crtStream" }
                conn.shutdown()
                cancelled = true
            }

            conn.close()
        }
    }
}
