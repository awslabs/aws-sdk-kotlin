/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.sdk.kotlin.services.s3.waiters.waitUntilBucketExists
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.text.ensurePrefix
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.OutputStreamWriter
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration.Companion.seconds

object S3TestUtils {

    const val DEFAULT_REGION = "us-west-2"

    private const val TEST_BUCKET_PREFIX = "s3-test-bucket-"

    private const val S3_MAX_BUCKET_NAME_LENGTH = 63 // https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html
    private const val S3_EXPRESS_DIRECTORY_BUCKET_SUFFIX = "--x-s3"

    suspend fun getTestBucket(client: S3Client): String = getBucketWithPrefix(client, TEST_BUCKET_PREFIX)

    suspend fun getTestDirectoryBucket(client: S3Client, suffix: String) = withTimeout(60.seconds) {
        var testBucket = client.listBuckets()
            .buckets
            ?.mapNotNull { it.name }
            ?.firstOrNull { it.startsWith(TEST_BUCKET_PREFIX) && it.endsWith(S3_EXPRESS_DIRECTORY_BUCKET_SUFFIX) }

        if (testBucket == null) {
            // Adding S3 Express suffix surpasses the bucket name length limit... trim the UUID if needed
            testBucket = TEST_BUCKET_PREFIX +
                UUID.randomUUID().toString().subSequence(0 until (S3_MAX_BUCKET_NAME_LENGTH - TEST_BUCKET_PREFIX.length - suffix.ensurePrefix("--").length)) +
                suffix.ensurePrefix("--")

            println("Creating S3 Express directory bucket: $testBucket")

            val availabilityZone = testBucket // s3-test-bucket-UUID--use1-az4--x-s3
                .removeSuffix(S3_EXPRESS_DIRECTORY_BUCKET_SUFFIX) // s3-test-bucket-UUID--use1-az4
                .substringAfterLast("--") // use1-az4

            client.createBucket {
                bucket = testBucket
                createBucketConfiguration {
                    location = LocationInfo {
                        type = LocationType.AvailabilityZone
                        name = availabilityZone
                    }
                    bucket = BucketInfo {
                        type = BucketType.Directory
                        dataRedundancy = DataRedundancy.SingleAvailabilityZone
                    }
                }
            }
        }
        testBucket
    }

    private suspend fun getBucketWithPrefix(client: S3Client, prefix: String): String = withTimeout(60.seconds) {
        var testBucket = client.listBuckets()
            .buckets
            ?.mapNotNull { it.name }
            ?.firstOrNull { it.startsWith(prefix) }

        if (testBucket == null) {
            testBucket = prefix + UUID.randomUUID()
            println("Creating S3 bucket: $testBucket")

            client.createBucket {
                bucket = testBucket
                createBucketConfiguration {
                    locationConstraint = BucketLocationConstraint.fromValue(client.config.region!!)
                }
            }

            client.waitUntilBucketExists { bucket = testBucket }
        } else {
            println("Using existing S3 bucket: $testBucket")
        }

        client.putBucketLifecycleConfiguration {
            bucket = testBucket
            lifecycleConfiguration {
                rules = listOf(
                    LifecycleRule {
                        expiration { days = 1 }
                        filter = LifecycleRuleFilter.Prefix("")
                        status = ExpirationStatus.Enabled
                        id = "delete-old"
                    },
                )
            }
        }

        testBucket
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun deleteBucketAndAllContents(client: S3Client, bucketName: String): Unit = coroutineScope {
        val scope = this

        try {
            println("Deleting S3 bucket: $bucketName")
            val dispatcher = Dispatchers.Default.limitedParallelism(64)
            val jobs = mutableListOf<Job>()

            client.listObjectsV2Paginated { bucket = bucketName }
                .mapNotNull { it.contents }
                .collect { contents ->
                    val job = scope.launch(dispatcher) {
                        client.deleteObjects {
                            bucket = bucketName
                            delete {
                                objects = contents.mapNotNull(Object::key).map { ObjectIdentifier { key = it } }
                            }
                        }
                    }
                    jobs.add(job)
                }

            jobs.joinAll()

            client.deleteBucket { bucket = bucketName }
        } catch (ex: Exception) {
            println("Failed to delete bucket: $bucketName")
            throw ex
        }
    }

    fun responseCodeFromPut(presignedRequest: HttpRequest, content: String): Int {
        val url = URL(presignedRequest.url.toString())
        val connection: HttpsURLConnection = url.openConnection() as HttpsURLConnection
        presignedRequest.headers.forEach { key, values ->
            connection.setRequestProperty(key, values.first())
        }

        connection.doOutput = true
        connection.requestMethod = "PUT"
        val out = OutputStreamWriter(connection.outputStream)
        out.write(content)
        out.close()

        if (connection.errorStream != null) {
            error("request failed: ${connection.errorStream?.bufferedReader()?.readText()}")
        }

        return connection.responseCode
    }
}
