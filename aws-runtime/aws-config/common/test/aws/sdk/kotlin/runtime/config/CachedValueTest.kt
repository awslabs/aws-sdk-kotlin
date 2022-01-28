/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class CachedValueTest {
    @Test
    fun testNull() = runSuspendTest {
        val epoch = Instant.fromEpochSeconds(0)
        val clock = ManualClock(epoch)
        val value = CachedValue<String>(null, clock = clock)

        assertTrue(value.isExpired())
        assertNull(value.get())
    }

    @Test
    fun testExpiration() = runSuspendTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + 10.seconds
        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, clock = clock)

        assertFalse(value.isExpired())
        assertEquals("foo", value.get())

        clock.advance(10.seconds)
        assertTrue(value.isExpired())
        assertNull(value.get())
    }

    @Test
    fun testExpirationBuffer() = runSuspendTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + 100.seconds
        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, bufferTime = 30.seconds, clock = clock)

        assertFalse(value.isExpired())
        assertEquals("foo", value.get())

        clock.advance(70.seconds)
        assertTrue(value.isExpired())
        assertNull(value.get())
    }

    @Test
    fun testGetOrLoad() = runSuspendTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + 100.seconds
        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, bufferTime = 30.seconds, clock = clock)

        var count = 0
        val mu = Mutex()
        val initializer = suspend {
            mu.withLock { count++ }
            ExpiringValue("bar", expiresAt + count.seconds * 100)
        }

        assertFalse(value.isExpired())
        assertEquals("foo", value.getOrLoad(initializer))
        assertEquals(0, count)

        // t = 90
        clock.advance(90.seconds)
        assertEquals("bar", value.getOrLoad(initializer))
        assertFalse(value.isExpired())
        assertEquals(1, count)

        // t = 180
        clock.advance(90.seconds)
        repeat(10) {
            async {
                value.getOrLoad(initializer)
            }
        }
        yield()
        assertEquals(2, count)
    }
}
