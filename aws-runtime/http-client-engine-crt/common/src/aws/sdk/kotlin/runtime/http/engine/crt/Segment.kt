/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.readFully

internal typealias Segment = SdkBuffer

/**
 * Create a segment from the given src [ByteArray] and mark the entire contents readable
 */
internal fun newReadableSegment(src: ByteArray): Segment = Segment.of(src).apply { commitWritten(src.size) }

internal fun Segment.copyTo(dest: SdkBuffer, limit: Int = Int.MAX_VALUE): Int {
    check(readRemaining > 0) { "nothing left to read from segment" }
    val wc = minOf(readRemaining, limit)
    readFully(dest, wc)
    return wc
}

internal fun Segment.copyTo(dest: ByteArray, offset: Int = 0, length: Int = dest.size - offset): Int {
    check(readRemaining > 0) { "nothing left to read from segment" }
    val wc = minOf(length, readRemaining)
    readFully(dest, offset, wc)
    return wc
}
