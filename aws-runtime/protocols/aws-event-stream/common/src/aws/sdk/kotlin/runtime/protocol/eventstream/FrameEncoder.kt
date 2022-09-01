/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.SdkByteChannel
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.bytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Transform the stream of messages into a stream of raw bytes. Each
 * element of the resulting flow is the encoded version of the corresponding message
 */
@InternalSdkApi
public fun Flow<Message>.encode(): Flow<ByteArray> = map {
    // TODO - can we figure out the encoded size and directly get a byte array
    val buffer = SdkByteBuffer(1024U)
    it.encode(buffer)
    buffer.bytes()
}

/**
 * Transform a stream of encoded messages into an [HttpBody].
 * @param scope parent scope to launch a coroutine in that consumes the flow and populates a [SdkByteReadChannel]
 */
@InternalSdkApi
public suspend fun Flow<ByteArray>.asEventStreamHttpBody(scope: CoroutineScope): HttpBody {
    val encodedMessages = this
    val ch = SdkByteChannel(true)

    return object : HttpBody.Streaming() {
        override val contentLength: Long? = null
        override val isReplayable: Boolean = false
        override val isDuplex: Boolean = true

        private var job: Job? = null

        override fun readFrom(): SdkByteReadChannel {
            // FIXME - delaying launch here until the channel is consumed from the HTTP engine is a hacky way
            //  of enforcing ordering to ensure the ExecutionContext is updated with the
            //  AwsSigningAttributes.RequestSignature by the time the messages are collected and sign() is called

            // Although rare, nothing stops downstream consumers from invoking readFrom() more than once.
            // Only launch background collection task on first call
            if (job == null) {
                job = scope.launch {
                    encodedMessages.collect {
                        ch.writeFully(it)
                    }
                }

                job?.invokeOnCompletion { cause ->
                    ch.close(cause)
                }
            }

            return ch
        }
    }
}
