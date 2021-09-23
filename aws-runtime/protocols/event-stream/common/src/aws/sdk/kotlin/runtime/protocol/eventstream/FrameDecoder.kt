/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.readFully

/**
 * Output of [FrameDecoder]
 */
@InternalSdkApi
public sealed class DecodedFrame {
    /**
     * There isn't enough data to decode a full message
     */
    public object Incomplete : DecodedFrame()

    /**
     * A message was successfully decoded
     */
    public data class Complete(val message: Message) : DecodedFrame()
}

@InternalSdkApi
public class FrameDecoder {
    private var prelude: Prelude? = null

    /**
     * Reset the decoder discarding any intermediate state
     */
    public fun reset() { prelude = null }

    private fun isFrameAvailable(buffer: SdkBuffer): Boolean {
        val totalLen = prelude?.totalLen ?: return false
        val remaining = totalLen - PRELUDE_BYTE_LEN_WITH_CRC
        return buffer.readRemaining >= remaining
    }

    /**
     * Attempt to decode a [Message] from the buffer. This function expects to be called over and over again
     * with more data in the buffer each time its called. When there is not enough data to decode this function
     * returns [DecodedFrame.Incomplete].
     * The decoder will consume the prelude when enough data is available. When it is invoked with enough
     * data it will consume the remaining message bytes.
     */
    public fun decodeFrame(buffer: SdkBuffer): DecodedFrame {
        if (prelude == null && buffer.readRemaining >= PRELUDE_BYTE_LEN_WITH_CRC) {
            prelude = Prelude.decode(buffer)
        }

        return when (isFrameAvailable(buffer)) {
            true -> {
                val currPrelude = checkNotNull(prelude)
                val messageBuf = SdkBuffer(currPrelude.totalLen)
                currPrelude.encode(messageBuf)
                buffer.readFully(messageBuf)
                reset()
                Message.decode(messageBuf).let { DecodedFrame.Complete(it) }
            }
            else -> DecodedFrame.Incomplete
        }
    }
}
