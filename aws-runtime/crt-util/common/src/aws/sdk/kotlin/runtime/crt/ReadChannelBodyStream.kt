/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.crt

import aws.sdk.kotlin.crt.http.HttpRequestBodyStream
import aws.sdk.kotlin.crt.io.MutableBuffer
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.readAvailable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext

/**
 * write as much of [outgoing] to [dest] as possible
 */
internal expect fun transferRequestBody(outgoing: SdkByteBuffer, dest: MutableBuffer)

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

    private val currBuffer = atomic<SdkByteBuffer?>(null)
    private val bufferChan = Channel<SdkByteBuffer>(Channel.UNLIMITED)

    init {
        producerJob.invokeOnCompletion { cause ->
            bodyChan.cancel(cause)
        }
    }

    // lie - CRT tries to control this via normal seek operations (e.g. when they calculate a hash for signing
    // they consume the aws_input_stream and then seek to the beginning). Instead we either support creating
    // a new read channel or we don't. At this level we don't care, consumers of this type need to understand
    // and handle these concerns.
    override fun resetPosition(): Boolean = true

    override fun sendRequestBody(buffer: MutableBuffer): Boolean {
        return doSendRequestBody(buffer).also { if (it) producerJob.complete() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun doSendRequestBody(buffer: MutableBuffer): Boolean {
        // ensure the request context hasn't been cancelled
        callContext.ensureActive()
        var outgoing = currBuffer.getAndSet(null) ?: bufferChan.tryReceive().getOrNull()

        if (bodyChan.availableForRead > 0 && outgoing == null) {
            // NOTE: It is critical that the coroutine launched doesn't actually suspend because it will never
            // get a chance to resume. The CRT will consume the dispatcher thread until the data has been read
            // completely. We could launch one of the coroutines into a different dispatcher but this won't work
            // on platforms (e.g. JS) that don't have multiple threads. Essentially the CRT will starve
            // the dispatcher and not allow other coroutines to make progress.
            // see: https://github.com/awslabs/aws-sdk-kotlin/issues/282
            //
            // TODO - we could get rid of this extra copy + coroutine if readAvailable() had a non-suspend version
            // see: https://youtrack.jetbrains.com/issue/KTOR-2772
            //
            // To get around this, if there is data to read we launch a coroutine UNDISPATCHED so that it runs
            // immediately in the current thread. The coroutine will fill the buffer but won't suspend because
            // we know data is available.
            launch(start = CoroutineStart.UNDISPATCHED) {
                val sdkBuffer = SdkByteBuffer(bodyChan.availableForRead.toULong())
                bodyChan.readAvailable(sdkBuffer)
                bufferChan.send(sdkBuffer)
            }.invokeOnCompletion { cause ->
                if (cause != null) {
                    producerJob.completeExceptionally(cause)
                    bufferChan.close(cause)
                }
            }
        }

        if (bodyChan.availableForRead == 0 && bodyChan.isClosedForRead) {
            bufferChan.close()
        }

        if (outgoing == null) {
            if (bufferChan.isClosedForReceive) {
                return true
            }

            outgoing = bufferChan.tryReceive().getOrNull() ?: return false
        }

        transferRequestBody(outgoing, buffer)

        if (outgoing.readRemaining > 0u) {
            currBuffer.value = outgoing
        }

        return bufferChan.isClosedForReceive && currBuffer.value == null
    }
}
