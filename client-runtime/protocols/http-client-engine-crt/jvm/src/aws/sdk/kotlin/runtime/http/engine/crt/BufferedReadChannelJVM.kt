/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import java.nio.ByteBuffer

internal actual fun bufferedReadChannel(onBytesRead: (n: Int) -> Unit): BufferedReadChannel =
    BufferedReadChannelImpl(onBytesRead)

internal class BufferedReadChannelImpl(
    onBytesRead: (n: Int) -> Unit
) : AbstractBufferedReadChannel(onBytesRead) {

    override suspend fun readAvailable(sink: ByteBuffer): Int {
        TODO("Not yet implemented")
    }
}
