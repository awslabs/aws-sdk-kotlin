/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.CRT
import aws.sdk.kotlin.crt.http.HttpHeader
import aws.sdk.kotlin.crt.http.HttpHeaderBlock
import aws.sdk.kotlin.crt.http.HttpStream
import aws.sdk.kotlin.crt.http.HttpStreamResponseHandler
import aws.sdk.kotlin.crt.io.Buffer
import aws.sdk.kotlin.runtime.ClientException
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import software.aws.clientrt.http.HeadersBuilder
import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.HttpStatusCode
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.io.SdkByteReadChannel

@OptIn(ExperimentalCoroutinesApi::class)
internal class SdkStreamResponseHandler(
    // FIXME - need to propagate this down? or can we tie it to a job cleanly?
    private val onResponseConsumed: () -> Unit = {},
): HttpStreamResponseHandler {
    // FIXME - need to cancel the stream when the body is closed from the caller side

    private val responseReady = Channel<HttpResponse>(1)
    private val headers = HeadersBuilder()
    private var status: Int? = null

    private var sdkBody: AbstractBufferedReadChannel? = null
    private val crtStream : AtomicRef<HttpStream?> = atomic(null)

    private val Int.isMainHeadersBlock: Boolean
        get() = when(this) {
            HttpHeaderBlock.MAIN.blockType -> true
            else -> false
        }

    private fun onDataConsumed(size: Int) {
        crtStream.value?.incrementWindow(size)
    }

    override fun onResponseHeaders(
        stream: HttpStream,
        responseStatusCode: Int,
        blockType: Int,
        nextHeaders: List<HttpHeader>?
    ) {
        crtStream.update { stream }
        if (!blockType.isMainHeadersBlock) return

        status = responseStatusCode
        nextHeaders?.forEach {
            headers.append(it.name, it.value)
        }
    }

    // signal response ready and engine can proceed (all that is required is headers, body is consumed asynchronously)
    private fun signalResponse() {
        // already signalled
        if (responseReady.isClosedForSend) return

        val body = object : HttpBody.Streaming() {
            override fun readFrom(): SdkByteReadChannel {
                if (sdkBody == null) {
                    // FIXME - we need to propagate client side close() up to the connection manager
                    sdkBody = bufferedReadChannel(::onDataConsumed)
                }

                return sdkBody!!
            }
        }

        val resp = HttpResponse(
            requireNotNull(status).let { HttpStatusCode.fromValue(it) },
            headers.build(),
            body
        )

        val result = responseReady.trySend(resp)
        check(result.isSuccess) { "signalling response failed, result was: ${result.exceptionOrNull()}" }
        responseReady.close()
    }

    override fun onResponseHeadersDone(stream: HttpStream, blockType: Int) {
        if (!blockType.isMainHeadersBlock) return
        signalResponse()
    }

    override fun onResponseBody(stream: HttpStream, bodyBytesIn: Buffer): Int {
        crtStream.update { stream }
        sdkBody?.write(bodyBytesIn)

        // explicit window management is done in BufferedReadChannel
        return 0
    }

    override fun onResponseComplete(stream: HttpStream, errorCode: Int) {
        // stream is only valid until the end of this callback
        crtStream.update{ null }

        // close the body channel
        if (errorCode != 0) {
            val errorDescription = CRT.errorString(errorCode)
            val ex = ClientException("CrtHttpEngine::response failed: ec=$errorCode; description=$errorDescription")
            responseReady.close(ex)
        } else {
            // ensure a response was signalled (will close the channel on it's own if it wasn't already sent)
            signalResponse()
        }
    }

    internal suspend fun waitForResponse(): HttpResponse {
        return responseReady.receive()
    }
}