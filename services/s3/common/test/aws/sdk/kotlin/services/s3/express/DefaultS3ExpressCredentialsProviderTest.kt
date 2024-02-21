/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Attributes
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateSessionRequest
import aws.sdk.kotlin.services.s3.model.CreateSessionResponse
import aws.sdk.kotlin.services.s3.model.SessionCredentials
import aws.sdk.kotlin.services.s3.withConfig
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class DefaultS3ExpressCredentialsProviderTest {
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
    fun testAsyncRefresh() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        // Entry expires in 30 seconds, refresh buffer is 1 minute. Next `resolve` call should trigger the async refresh
        val cache = S3ExpressCredentialsCache()
        val entry = getCacheEntry(timeSource.markNow() + 30.seconds)
        cache.put(entry.key, entry.value)

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        val testClient = TestS3Client(expectedCredentials)

        DefaultS3ExpressCredentialsProvider(timeSource, clock, cache, refreshBuffer = 1.minutes).use { provider ->
            val attributes = ExecutionContext.build {
                this.attributes[S3Attributes.ExpressClient] = testClient
                this.attributes[S3Attributes.Bucket] = "bucket"
            }

            provider.resolve(attributes)
            assertEquals(1, testClient.numCreateSession)
        }
    }

    @Test
    fun testAsyncRefreshDebounce() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        // Entry expires in 30 seconds, refresh buffer is 1 minute. Next `resolve` call should trigger the async refresh
        val cache = S3ExpressCredentialsCache()
        val entry = getCacheEntry(expiration = timeSource.markNow() + 30.seconds)
        cache.put(entry.key, entry.value)

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        val testClient = TestS3Client(expectedCredentials)

        DefaultS3ExpressCredentialsProvider(timeSource, clock, cache, refreshBuffer = 1.minutes).use { provider ->
            val attributes = ExecutionContext.build {
                this.attributes[S3Attributes.ExpressClient] = testClient
                this.attributes[S3Attributes.Bucket] = "bucket"
            }

            // launch many async `resolve` calls, only one should call s3:CreateSession
            val calls = (1..5).map {
                async { provider.resolve(attributes) }
            }
            calls.awaitAll()
            assertEquals(1, testClient.numCreateSession)
        }
    }

    @Test
    fun testAsyncRefreshHandlesFailures() = runTest {
        val timeSource = TestTimeSource()
        val clock = ManualClock()

        // Entry expires in 30 seconds, refresh buffer is 1 minute. Next `resolve` call should trigger the async refresh
        val cache = S3ExpressCredentialsCache()
        val successEntry = getCacheEntry(timeSource.markNow() + 30.seconds, bucket = "SuccessfulBucket", bootstrapCredentials = Credentials("1", "1", "1"))
        val failedEntry = getCacheEntry(timeSource.markNow() + 30.seconds, bucket = "ExceptionBucket", bootstrapCredentials = Credentials("1", "1", "1"))
        cache.put(successEntry.key, successEntry.value)
        cache.put(failedEntry.key, failedEntry.value)

        val expectedCredentials = SessionCredentials {
            accessKeyId = "access"
            secretAccessKey = "secret"
            sessionToken = "session"
            expiration = clock.now() + 5.minutes
        }

        // client will throw an exception when `ExceptionBucket` credentials are fetched,
        // but there should be no crash
        val testClient = TestS3Client(expectedCredentials, throwExceptionOnBucketNamed = "ExceptionBucket", baseCredentials = Credentials("1", "1", "1"))

        DefaultS3ExpressCredentialsProvider(timeSource, clock, cache, refreshBuffer = 1.minutes).use { provider ->
            val attributes = ExecutionContext.build {
                this.attributes[S3Attributes.ExpressClient] = testClient
                this.attributes[S3Attributes.Bucket] = "ExceptionBucket"
            }
            provider.resolve(attributes)

            attributes[S3Attributes.Bucket] = "SuccessfulBucket"
            provider.resolve(attributes)
        }

        // close the provider, make sure all async refreshes are complete...
        assertEquals(2, testClient.numCreateSession)
    }

    /**
     * Get an instance of [Map.Entry<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>] using the given [expiration],
     * [bucket], and optional [bootstrapCredentials] and [sessionCredentials].
     */
    private fun getCacheEntry(
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
     * @param throwExceptionOnBucketNamed an optional bucket name, which when specified and present in the [CreateSessionRequest], will
     * cause the client to throw an exception instead of returning credentials. Used for testing s3:CreateSession failures.
     */
    private class TestS3Client(
        val expectedCredentials: SessionCredentials,
        val client: S3Client = S3Client { },
        val baseCredentials: Credentials? = null,
        val throwExceptionOnBucketNamed: String? = null,
    ) : S3Client by client {
        var numCreateSession = 0
        override val config: S3Client.Config
            get() = baseCredentials?.let {
                client.withConfig {
                    credentialsProvider = StaticCredentialsProvider(baseCredentials)
                }.config
            } ?: client.config

        override suspend fun createSession(input: CreateSessionRequest): CreateSessionResponse {
            println("in createSession for $input")
            numCreateSession += 1
            println("numCreateSession: $numCreateSession")

            throwExceptionOnBucketNamed?.let {
                if (input.bucket == it) {
                    throw Exception("Failed to create session credentials for bucket: $throwExceptionOnBucketNamed")
                }
            }
            return CreateSessionResponse { credentials = expectedCredentials }
        }
    }
}
