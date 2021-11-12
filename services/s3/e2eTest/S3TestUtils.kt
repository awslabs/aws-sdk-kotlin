/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.ExpirationStatus
import aws.sdk.kotlin.services.s3.model.LifecycleRule
import aws.sdk.kotlin.services.s3.model.LifecycleRuleFilter
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.OutputStreamWriter
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

object S3TestUtils {

    const val TEST_BUCKET_PREFIX = "s3-test-bucket-"

    suspend fun getTestBucket(client: S3Client): String = getBucketWithPrefix(client, TEST_BUCKET_PREFIX)

    @OptIn(ExperimentalTime::class)
    suspend fun getBucketWithPrefix(client: S3Client, prefix: String): String = withTimeout(Duration.seconds(60)) {
        var testBucket = client.listBuckets {}
            .buckets
            ?.mapNotNull { it.name }
            ?.firstOrNull { it.startsWith(prefix) }

        if (testBucket == null) {
            testBucket = prefix + UUID.randomUUID()
            client.createBucket {
                bucket = testBucket
                createBucketConfiguration {
                    locationConstraint = BucketLocationConstraint.fromValue(client.config.region!!)
                }
            }

            do {
                val bucketExists = try {
                    client.headBucket { bucket = testBucket }
                    true
                } catch (ex: NotFound) {
                    delay(300)
                    false
                }
            } while (!bucketExists)
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
                    }
                )
            }
        }

        testBucket
    }

    suspend fun deleteBucketAndAllContents(client: S3Client, bucketName: String) {
        try {
            println("Deleting S3 bucket: $bucketName")

            var resp = client.listObjectsV2 { bucket = bucketName }

            do {
                val objects = resp.contents
                val truncated = resp.isTruncated

                objects?.forEach {
                    client.deleteObject {
                        bucket = bucketName
                        key = it.key
                    }
                }

                if (truncated) {
                    resp = client.listObjectsV2 {
                        bucket = bucketName
                        continuationToken = resp.continuationToken
                    }
                }
            } while (truncated)

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
