/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CachedCredentialsProviderTest {
    private val epoch = Instant.fromIso8601("2020-10-16T03:56:00Z")
    private val testExpiration = epoch + Duration.minutes(30)
    private val testClock = ManualClock(epoch)

    private class TestCredentialsProvider(
        private val expiration: Instant? = null
    ) : CredentialsProvider {
        var callCount = 0

        override suspend fun getCredentials(): Credentials {
            callCount++
            return Credentials(
                "AKID",
                "secret",
                expiration = this@TestCredentialsProvider.expiration
            )
        }
    }

    @Test
    fun testLoadFirstCall(): Unit = runSuspendTest {
        // explicit expiration
        val source = TestCredentialsProvider(expiration = testExpiration)
        val provider = CachedCredentialsProvider(source, clock = testClock)
        val creds = provider.getCredentials()
        val expected = Credentials("AKID", "secret", expiration = testExpiration)
        assertEquals(expected, creds)
        assertEquals(1, source.callCount)

        provider.getCredentials()
        assertEquals(1, source.callCount)
    }

    @Test
    fun testDefaultExpiration(): Unit = runSuspendTest {
        // expiration should come from the cached provider
        val source = TestCredentialsProvider()
        val provider = CachedCredentialsProvider(source, clock = testClock)
        val creds = provider.getCredentials()
        val expectedExpiration = epoch + Duration.minutes(15)
        val expected = Credentials("AKID", "secret", expiration = expectedExpiration)
        assertEquals(expected, creds)
        assertEquals(1, source.callCount)
    }

    @Test
    fun testReloadExpiredCredentials(): Unit = runSuspendTest {
        val source = TestCredentialsProvider(expiration = testExpiration)
        val provider = CachedCredentialsProvider(source, clock = testClock)
        val creds = provider.getCredentials()
        val expected = Credentials("AKID", "secret", expiration = testExpiration)
        assertEquals(expected, creds)
        assertEquals(1, source.callCount)

        // 1 min past expiration
        testClock.advance(Duration.minutes(31))
        provider.getCredentials()
        assertEquals(2, source.callCount)
    }

    @Test
    fun testRefreshBufferWindow(): Unit = runSuspendTest {
        val source = TestCredentialsProvider(expiration = testExpiration)
        val provider = CachedCredentialsProvider(source, clock = testClock)
        val creds = provider.getCredentials()
        val expected = Credentials("AKID", "secret", expiration = testExpiration)
        assertEquals(expected, creds)
        assertEquals(1, source.callCount)

        // default buffer window is 10 seconds, advance 29 minutes, 49 seconds
        testClock.advance(Duration.seconds(29 * 60 + 49))
        provider.getCredentials()
        // not within window yet
        assertEquals(1, source.callCount)

        // now we should be within 10 sec window
        testClock.advance(Duration.seconds(1))
        provider.getCredentials()
        assertEquals(2, source.callCount)
    }

    @Test
    fun testLoadFailed(): Unit = runSuspendTest {
        val source = object : CredentialsProvider {
            private var count = 0
            override suspend fun getCredentials(): Credentials {
                if (count <= 0) {
                    count++
                    throw RuntimeException("test error")
                }
                return Credentials("AKID", "secret")
            }
        }
        val provider = CachedCredentialsProvider(source, clock = testClock)

        assertFailsWith<RuntimeException> {
            provider.getCredentials()
        }.message.shouldContain("test error")

        // future successful invocations should continue to work
        provider.getCredentials()
    }
}
