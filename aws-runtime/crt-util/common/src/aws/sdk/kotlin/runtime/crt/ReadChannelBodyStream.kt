/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt

import aws.sdk.kotlin.crt.http.HttpRequestBodyStream
import aws.sdk.kotlin.crt.io.MutableBuffer
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.readAvailable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext

/**
 * write as much of [outgoing] to [dest] as possible
 */
internal expect fun transferRequestBody(outgoing: SdkBuffer, dest: MutableBuffer)

/**
 * Implement's [HttpRequestBodyStream] which proxies an SDK request body channel [SdkByteReadChannel]
 */
@InternalSdkApi
public class ReadChannelBodyStream(
    // the request body channel
    private val bodyChan: SdkByteReadChannel,
    private val callContext: CoroutineContext
) : HttpRequestBodyStream, CoroutineScope {

    private val producerJob = Job(callContext.job)
    override val coroutineContext: CoroutineContext = callContext + producerJob

    private val currBuffer = atomic<SdkBuffer?>(null)
    private val bufferChan = Channel<SdkBuffer>(1)

    init {
        producerJob.invokeOnCompletion { cause ->
            bodyChan.cancel(cause)
        }

        // launch a coroutine to fill the buffer channel
        proxyRequestBody()
    }

    // lie - CRT tries to control this via normal seek operations (e.g. when they calculate a hash for signing
    // they consume the aws_input_stream and then seek to the beginning). Instead we either support creating
    // a new read channel or we don't. At this level we don't care, consumers of this type need to understand
    // and handle these concerns.
    override fun resetPosition(): Boolean = true

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun sendRequestBody(buffer: MutableBuffer): Boolean {
        var outgoing = currBuffer.getAndSet(null) ?: bufferChan.tryReceive().getOrNull()

        if (outgoing == null) {
            if (bufferChan.isClosedForReceive) {
                return true
            }
            // ensure the request context hasn't been cancelled
            callContext.ensureActive()
            outgoing = bufferChan.tryReceive().getOrNull() ?: return false
        }

        transferRequestBody(outgoing, buffer)

        if (outgoing.readRemaining > 0) {
            currBuffer.value = outgoing
        }

        return bufferChan.isClosedForReceive && currBuffer.value == null
    }

    private fun proxyRequestBody() {
        // TODO - we could get rid of this extra copy + coroutine if readAvailable() had a non-suspend version
        // see: https://youtrack.jetbrains.com/issue/KTOR-2772
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            while (!bodyChan.isClosedForRead) {
                bodyChan.awaitContent()
                if (bodyChan.isClosedForRead) return@launch

                // TODO - we could pool these
                val buffer = SdkBuffer(bodyChan.availableForRead)
                bodyChan.readAvailable(buffer)
                bufferChan.send(buffer)
            }
        }

        job.invokeOnCompletion { cause ->
            bufferChan.close(cause)
            if (cause != null) {
                producerJob.completeExceptionally(cause)
            } else {
                producerJob.complete()
            }
        }
    }
}
