/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentTest {
    @Test
    fun testCopyToByteArray() {
        val segment = Segment("1234".encodeToByteArray())
        val dest = ByteArray(16)
        val rc = segment.copyTo(dest)
        assertEquals(4, rc)
        assertEquals("1234", dest.decodeToString(0, 4))
    }
}
