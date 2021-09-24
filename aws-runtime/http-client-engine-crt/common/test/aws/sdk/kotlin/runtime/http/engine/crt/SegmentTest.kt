/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.decodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentTest {
    @Test
    fun testCopyToByteArray() {
        val segment = newReadableSegment("1234".encodeToByteArray())
        val dest = ByteArray(16)
        val rc = segment.copyTo(dest)
        assertEquals(4, rc)
        assertEquals("1234", dest.decodeToString(0, 4))
    }

    @Test
    fun testCopyToSdkBuffer() {
        val segment = newReadableSegment("1234".encodeToByteArray())
        val dest = SdkByteBuffer(16u)
        val rc = segment.copyTo(dest)
        assertEquals(4, rc)
        assertEquals(4u, dest.writePosition)
        assertEquals(4u, dest.readRemaining)
        assertEquals("1234", dest.decodeToString())
    }
}
