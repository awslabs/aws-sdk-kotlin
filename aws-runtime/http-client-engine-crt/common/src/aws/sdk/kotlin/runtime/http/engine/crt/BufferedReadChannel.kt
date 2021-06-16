/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.io.Buffer
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel

/**
 * Create a new [BufferedReadChannel] that invokes [onBytesRead] as data is consumed
 */
internal expect fun bufferedReadChannel(onBytesRead: (n: Int) -> Unit): BufferedReadChannel

/**
 * A buffered [SdkByteReadChannel] that can always satisfy writing without blocking / suspension
 */
internal interface BufferedReadChannel : SdkByteReadChannel {
    /**
     * Write the data from the buffer to the channel IMMEDIATELY without blocking or suspension
     */
    fun write(data: Buffer)
}
