/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateSessionRequest
import aws.sdk.kotlin.services.s3.model.CreateSessionResponse
import aws.sdk.kotlin.services.s3.model.SessionCredentials
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class DefaultS3ExpressCredentialsProviderTest {
    @Test
    fun testGetNextRefreshDuration() = runTest {
        val timeSource = TestTimeSource()

        val entries = mutableListOf(
            getEntry(timeSource.markNow() + 5.minutes),
            getEntry(timeSource.markNow() + 6.minutes),
            getEntry(timeSource.markNow() + 7.minutes),
        )

        timeSource += 2.minutes

        DefaultS3ExpressCredentialsProvider(timeSource).use {
            // earliest expiration in 3 minutes, minus a 1 minute buffer = 2 minutes
            assertEquals(2.minutes, it.getNextRefreshDuration(entries))

            // Empty list of entries should result in the default refresh period
            assertEquals(DEFAULT_REFRESH_PERIOD, it.getNextRefreshDuration(listOf()))
        }
    }

    @Test
    fun testCreateSessionCredentials() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        val client = TestS3Client(expectedCredentials)

        DefaultS3ExpressCredentialsProvider(timeSource, clock).use { provider ->
            val credentials = provider.createSessionCredentials("bucket", client)
            assertFalse(credentials.isExpired)
            assertEquals(timeSource.markNow() + 5.minutes, credentials.expiresAt)
        }
    }

    @Test
    fun testRefreshExpiredEntries() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()
        val cache = S3ExpressCredentialsCache()
        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        val expiredEntries = listOf(
            getEntry(timeSource.markNow(), bootstrapCredentials = Credentials("1", "1", "1")),
            getEntry(timeSource.markNow() + 30.seconds, bootstrapCredentials = Credentials("2", "2", "2")),
            getEntry(timeSource.markNow() + 1.minutes, bootstrapCredentials = Credentials("3", "3", "3")),
            getEntry(timeSource.markNow() + 1.minutes + 30.seconds, bootstrapCredentials = Credentials("4", "4", "4")),
            getEntry(timeSource.markNow() + 2.minutes, bootstrapCredentials = Credentials("5", "5", "5")),
            getEntry(timeSource.markNow() + 2.minutes + 30.seconds, bootstrapCredentials = Credentials("6", "6", "6")),
        )
        timeSource += 3.minutes
        assertTrue(expiredEntries.all { it.value.expiringCredentials.isExpired }) // validate all entries are now expired

        val testS3Client = TestS3Client(expectedCredentials)

        DefaultS3ExpressCredentialsProvider(timeSource, clock, cache).use { provider ->
            provider.refreshExpiredEntries(expiredEntries, testS3Client)
        }

        val refreshedEntries = cache.entries
        assertFalse(refreshedEntries.any { it.value.expiringCredentials.isExpired }) // none of the entries are expired
        assertFalse(refreshedEntries.any { it.value.usedSinceLastRefresh }) // none of the entries have been used since the last refresh
    }

    @Test
    fun testRefreshExpiredEntriesHandlesFailures() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()
        val cache = S3ExpressCredentialsCache()

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }
        val testS3Client = TestS3Client(expectedCredentials, throwExceptionOnBucket = "ExceptionBucket")

        val expiredEntries = listOf(
            getEntry(timeSource.markNow() + 30.seconds, bucket = "ExceptionBucket", bootstrapCredentials = Credentials("1", "1", "1")),
            getEntry(timeSource.markNow() + 1.minutes, bucket = "SuccessfulBucket", bootstrapCredentials = Credentials("2", "2", "2")),
            getEntry(timeSource.markNow() + 1.minutes + 30.seconds, bucket = "SuccessfulBucket", bootstrapCredentials = Credentials("3", "3", "3")),
            getEntry(timeSource.markNow() + 2.minutes, bucket = "ExceptionBucket", bootstrapCredentials = Credentials("4", "4", "4")),
            getEntry(timeSource.markNow(), bucket = "SuccessfulBucket", bootstrapCredentials = Credentials("5", "5", "5")),
        )
        expiredEntries.forEach { cache.put(it.key, it.value) }
        assertEquals(5, cache.size)

        timeSource += 3.minutes
        assertTrue(expiredEntries.all { it.value.expiringCredentials.isExpired }) // all entries are now expired

        DefaultS3ExpressCredentialsProvider(timeSource, clock, cache).use { provider ->
            provider.refreshExpiredEntries(expiredEntries, testS3Client)
        }

        val refreshedEntries = cache.entries
        assertEquals(5, refreshedEntries.size) // no entries were removed

        // two entries failed to refresh, they are still expired.
        assertEquals(
            2,
            refreshedEntries
                .filter { it.value.expiringCredentials.isExpired }
                .size,
        )
        assertTrue(
            refreshedEntries
                .filter { it.value.expiringCredentials.isExpired }
                .all { it.key.bucket == "ExceptionBucket" },
        )

        // three entries successfully refreshed and are no longer expired
        assertEquals(
            3,
            refreshedEntries
                .filter { !it.value.expiringCredentials.isExpired }
                .size,
        )
        assertTrue(
            refreshedEntries
                .filter { !it.value.expiringCredentials.isExpired }
                .all { it.key.bucket == "SuccessfulBucket" },
        )
    }

    /**
     * Get an instance of [Map.Entry<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>] using the given [expiration],
     * [bucket], and optional [bootstrapCredentials] and [sessionCredentials].
     */
    private fun getEntry(
        expiration: ComparableTimeMark,
        bucket: String = "bucket",
        bootstrapCredentials: Credentials = Credentials(accessKeyId = "accessKeyId", secretAccessKey = "secretAccessKey", sessionToken = "sessionToken"),
        sessionCredentials: Credentials = Credentials(accessKeyId = "s3AccessKeyId", secretAccessKey = "s3SecretAccessKey", sessionToken = "s3SessionToken"),
    ): S3ExpressCredentialsCacheEntry = mapOf(
        S3ExpressCredentialsCacheKey(bucket, bootstrapCredentials) to S3ExpressCredentialsCacheValue(ExpiringValue(sessionCredentials, expiration)),
    ).entries.first()

    /**
     * A test S3Client used to mock calls to s3:CreateSession.
     * @param expectedCredentials the expected session credentials returned from s3:CreateSession
     * @param client the base S3 client used to implement other operations, though they are unused.
     * @param throwExceptionOnBucket an optional bucket name, which when specified and present in the [CreateSessionRequest], will
     * cause the client to throw an exception instead of returning credentials. Used for testing s3:CreateSession failures.
     */
    private class TestS3Client(
        val expectedCredentials: SessionCredentials,
        val client: S3Client = S3Client { },
        val throwExceptionOnBucket: String? = null,
    ) : S3Client by client {
        override suspend fun createSession(input: CreateSessionRequest): CreateSessionResponse {
            throwExceptionOnBucket?.let {
                if (input.bucket == it) {
                    throw Exception("Failed to create session credentials for bucket: $throwExceptionOnBucket")
                }
            }
            return CreateSessionResponse { credentials = expectedCredentials }
        }
    }
}
