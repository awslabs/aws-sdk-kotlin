/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.http.HttpRequestBodyStream
import aws.sdk.kotlin.crt.io.MutableBuffer
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import software.aws.clientrt.io.SdkByteReadChannel
import kotlin.coroutines.CoroutineContext

private const val DEFAULT_BUFFER_SIZE = 4096

/**
 * Proxy an SDK request body channel, [SdkByteReadChannel], as a CRT [HttpRequestBodyStream]
 */
internal class ReadChannelBodyStream(
    // the request body channel
    private val bodyChan: SdkByteReadChannel,
    callContext: CoroutineContext
): HttpRequestBodyStream, CoroutineScope {

    private val producerJob = Job(callContext.job)
    override val coroutineContext: CoroutineContext = callContext + producerJob

    private var currBuffer = atomic<ReadBuffer?>(null)
    private val bufferChan = Channel<ReadBuffer>(1)

    init {
        producerJob.invokeOnCompletion { cause ->
            bodyChan.cancel(cause)
        }

        // launch a coroutine to fill the buffer channel
        proxyRequestBody()
    }

    // FIXME - we don't close this on error/incomplete read ? need to propagate back to the caller


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun sendRequestBody(buffer: MutableBuffer): Boolean {
        var outgoing = currBuffer.getAndSet(null) ?: bufferChan.poll()

        if (outgoing == null) {
            if (bufferChan.isClosedForReceive) {
                return true
            }
            outgoing = bufferChan.poll() ?: return false
        }

        outgoing.writeTo(buffer)

        if (outgoing.readRemaining > 0) {
            currBuffer.value = outgoing
        }

        return bufferChan.isClosedForReceive
    }

    private fun proxyRequestBody() {
        val job = launch {
            while (!bodyChan.isClosedForRead) {
                // TODO - we could pool these
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val rc = bodyChan.readAvailable(buffer)
                bufferChan.send(ReadBuffer(buffer, limit = rc))
            }
        }

        job.invokeOnCompletion { cause ->
            bufferChan.close(cause)
            if (cause != null) {
                producerJob.completeExceptionally(cause)
            }else {
                producerJob.complete()
            }
        }
    }
}

internal class ReadBuffer(
    private val buf: ByteArray,
    offset: Int = 0,
    limit: Int = buf.size
) {
    var readHead: Int = offset

    /**
     * The total size of the buffer
     */
    val size: Int = limit

    /**
     * Number of bytes available for reading
     */
    val readRemaining: Int
        get() = size - readHead

    fun writeTo(buffer: MutableBuffer) {
        val written = buffer.write(buf, readHead, size)
        readHead += written
    }
}


