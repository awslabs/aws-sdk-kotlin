/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CachedValueTest {
    @Test
    fun testNull() = runTest {
        val epoch = Instant.fromEpochSeconds(0)
        val clock = ManualClock(epoch)
        val value = CachedValue<String>(null, clock = clock)

        assertTrue(value.isExpired)
        assertNull(value.get())
    }

    @Test
    fun testExpiration() = runTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + 10.seconds
        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, clock = clock)

        assertFalse(value.isExpired)
        assertEquals("foo", value.get())

        clock.advance(10.seconds)
        assertTrue(value.isExpired)
        assertNull(value.get())
    }

    @Test
    fun testExpirationBuffer() = runTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + 100.seconds
        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, bufferTime = 30.seconds, clock = clock)

        assertFalse(value.isExpired)
        assertEquals("foo", value.get())

        clock.advance(70.seconds)
        assertTrue(value.isExpired)
        assertNull(value.get())
    }

    @Test
    fun testGetOrLoad() = runTest {
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

        assertFalse(value.isExpired)
        assertEquals("foo", value.getOrLoad(initializer))
        assertEquals(0, count)

        // t = 90
        clock.advance(90.seconds)
        assertEquals("bar", value.getOrLoad(initializer))
        assertFalse(value.isExpired)
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

    @Test
    fun testClose() = runTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + 100.seconds

        val clock = ManualClock(epoch)

        val value = CachedValue("foo", expiresAt, bufferTime = 30.seconds, clock = clock)

        assertNotNull(value.get())
        value.close()
        assertFailsWith<IllegalStateException> { value.get() }
    }

    @Test
    fun throwsAfterCloseDuringGetOrLoad() = runTest {
        val epoch = Instant.fromEpochSeconds(0)
        val expiresAt = epoch + 100.seconds

        val value: CachedValue<String> = CachedValue()

        launch {
            assertFailsWith<IllegalStateException> {
                value.getOrLoad {
                    delay(5000)
                    ExpiringValue("bar", expiresAt)
                }
            }
        }

        delay(1000)
        value.close()
    }
}
