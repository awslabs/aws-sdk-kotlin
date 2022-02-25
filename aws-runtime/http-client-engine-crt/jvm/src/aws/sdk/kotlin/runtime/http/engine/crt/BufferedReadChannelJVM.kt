/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.of
import java.nio.ByteBuffer

internal actual fun bufferedReadChannel(onBytesRead: (n: Int) -> Unit): BufferedReadChannel =
    BufferedReadChannelImpl(onBytesRead)

internal class BufferedReadChannelImpl(
    onBytesRead: (n: Int) -> Unit
) : AbstractBufferedReadChannel(onBytesRead) {

    override suspend fun readAvailable(sink: ByteBuffer): Int {
        if (sink.remaining() == 0) return 0
        val sdkSink = SdkByteBuffer.of(sink)
        val consumed = readAsMuchAsPossible(sdkSink, sink.remaining())
        return when {
            consumed == 0 && closed != null -> -1
            consumed > 0 -> {
                sink.position(sink.position() + consumed)
                consumed
            }
            else -> readAvailableSuspend(sink)
        }
    }

    private suspend fun readAvailableSuspend(dest: ByteBuffer): Int {
        if (!readSuspend()) {
            return -1
        }
        return readAvailable(dest)
    }
}
