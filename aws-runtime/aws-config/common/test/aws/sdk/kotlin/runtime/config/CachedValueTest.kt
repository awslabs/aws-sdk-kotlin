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
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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
        val expiresAt = epoch + Duration.seconds(10)
        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, clock = clock)

        assertFalse(value.isExpired())
        assertEquals("foo", value.get())

        clock.advance(Duration.seconds(10))
        assertTrue(value.isExpired())
        assertNull(value.get())
    }

    @Test
    fun testExpirationBuffer() = runSuspendTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + Duration.seconds(100)
        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, bufferTime = Duration.seconds(30), clock = clock)

        assertFalse(value.isExpired())
        assertEquals("foo", value.get())

        clock.advance(Duration.seconds(70))
        assertTrue(value.isExpired())
        assertNull(value.get())
    }

    @Test
    fun testGetOrLoad() = runSuspendTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + Duration.seconds(100)
        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, bufferTime = Duration.seconds(30), clock = clock)

        var count = 0
        val mu = Mutex()
        val initializer = suspend {
            mu.withLock { count++ }
            ExpiringValue("bar", expiresAt + Duration.seconds(count * 100))
        }

        assertFalse(value.isExpired())
        assertEquals("foo", value.getOrLoad(initializer))
        assertEquals(0, count)

        // t = 90
        clock.advance(Duration.seconds(90))
        assertEquals("bar", value.getOrLoad(initializer))
        assertFalse(value.isExpired())
        assertEquals(1, count)

        // t = 180
        clock.advance(Duration.seconds(90))
        repeat(10) {
            async {
                value.getOrLoad(initializer)
            }
        }
        yield()
        assertEquals(2, count)
    }
}
