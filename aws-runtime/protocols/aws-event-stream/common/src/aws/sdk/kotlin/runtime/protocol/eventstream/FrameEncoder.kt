/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.bytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
