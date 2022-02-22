/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.Uuid
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

@OptIn(Uuid.WeakRng::class)
class HeaderValueTest {
    @Test
    fun testExpectAs() {
        assertEquals(true, HeaderValue.Bool(true).expectBool())
        assertEquals(12.toByte(), HeaderValue.Byte(12u).expectByte())
        assertEquals(12.toShort(), HeaderValue.Int16(12).expectInt16())
        assertEquals(12, HeaderValue.Int32(12).expectInt32())
        assertEquals(12L, HeaderValue.Int64(12L).expectInt64())
        assertEquals("foo", HeaderValue.String("foo").expectString())
        assertContentEquals("foo".encodeToByteArray(), HeaderValue.ByteArray("foo".encodeToByteArray()).expectByteArray())
        val ts = Instant.now()
        assertEquals(ts, HeaderValue.Timestamp(ts).expectTimestamp())
        val uuid = Uuid.random()
        assertEquals(uuid, HeaderValue.Uuid(uuid).expectUuid())

        assertFails {
            HeaderValue.Int32(12).expectString()
        }.message.shouldContain("expected HeaderValue.String, found: Int32(value=12)")
    }
}
