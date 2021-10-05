/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.readFully

internal typealias Segment = SdkByteBuffer

/**
 * Create a segment from the given src [ByteArray] and mark the entire contents readable
 */
internal fun newReadableSegment(src: ByteArray): Segment = Segment.of(src).apply { advance(src.size.toULong()) }

internal fun Segment.copyTo(dest: SdkByteBuffer, limit: Int = Int.MAX_VALUE): Int {
    check(readRemaining > 0u) { "nothing left to read from segment" }
    val wc = minOf(readRemaining, limit.toULong())
    readFully(dest, wc)
    return wc.toInt()
}

internal fun Segment.copyTo(dest: ByteArray, offset: Int = 0, length: Int = dest.size - offset): Int {
    check(readRemaining > 0u) { "nothing left to read from segment" }
    val wc = minOf(length.toULong(), readRemaining).toInt()
    readFully(dest, offset, wc)
    return wc
}
