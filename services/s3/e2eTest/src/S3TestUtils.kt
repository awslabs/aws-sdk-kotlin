/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.CreateBucketConfiguration
import aws.sdk.kotlin.services.s3.model.CreateBucketRequest
import aws.sdk.kotlin.services.s3.model.DeleteBucketRequest
import aws.sdk.kotlin.services.s3.model.ExpirationStatus
import aws.sdk.kotlin.services.s3.model.LifecycleRule
import aws.sdk.kotlin.services.s3.model.LifecycleRuleFilter
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.sdk.kotlin.services.s3.waiters.waitUntilBucketExists
import aws.sdk.kotlin.services.s3control.S3ControlClient
import aws.sdk.kotlin.services.s3control.model.*
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.OutputStreamWriter
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration.Companion.seconds

object S3TestUtils {

    const val DEFAULT_REGION = "us-west-2"

    private const val TEST_BUCKET_PREFIX = "s3-test-bucket-"

    suspend fun getTestBucket(client: S3Client): String = getBucketWithPrefix(client, TEST_BUCKET_PREFIX)

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

internal suspend fun getAccountId(): String {
    val sts = StsClient {
        region = "us-west-2"
    }

    val accountId = sts.getCallerIdentity(
        GetCallerIdentityRequest { },
    ).account

    sts.close()

    return accountId ?: throw Exception("Unable to get AWS account ID")
}

internal suspend fun createS3Bucket(
    s3Client: S3Client,
    bucketName: String,
    location: BucketLocationConstraint,
) {
    s3Client.createBucket(
        CreateBucketRequest {
            bucket = bucketName
            createBucketConfiguration =
                CreateBucketConfiguration {
                    locationConstraint = location
                }
        },
    )
}

internal suspend fun createMultiRegionAccessPoint(
    s3ControlClient: S3ControlClient,
    multiRegionAccessPointName: String,
    regionOneBucket: String,
    regionTwoBucket: String,
    testAccountId: String,
) {
    val createRequestToken = s3ControlClient.createMultiRegionAccessPoint(
        CreateMultiRegionAccessPointRequest {
            accountId = testAccountId
            details =
                CreateMultiRegionAccessPointInput {
                    name = multiRegionAccessPointName
                    regions =
                        listOf(
                            Region {
                                bucket = regionOneBucket
                            },
                            Region {
                                bucket = regionTwoBucket
                            },
                        )
                }
        },
    )

    waitUntilMultiRegionAccessPointOperationCompletes(
        s3ControlClient,
        createRequestToken.requestTokenArn ?: throw Exception("Unable to get request token ARN"),
        1000 * 60 * 10, // 10 minutes
        testAccountId,
        "createMultiRegionAccessPoint",
    )
}

internal suspend fun getMultiRegionAccessPointArn(
    s3ControlClient: S3ControlClient,
    multiRegionAccessPointName: String,
    testAccountId: String,
): String {
    s3ControlClient.listMultiRegionAccessPoints(
        ListMultiRegionAccessPointsRequest {
            accountId = testAccountId
        },
    ).accessPoints?.find { it.name == multiRegionAccessPointName }?.alias?.let { alias ->
        return "arn:aws:s3::$testAccountId:accesspoint/$alias"
    }
    throw Exception("Unable to get multi region access point arn")
}

internal suspend fun createObject(
    s3Client: S3Client,
    bucketName: String,
    keyName: String,
) {
    s3Client.putObject(
        PutObjectRequest {
            bucket = bucketName
            key = keyName
        },
    )
}

internal suspend fun deleteObject(
    s3Client: S3Client,
    bucketName: String,
    keyName: String,
) {
    s3Client.deleteObject(
        DeleteObjectRequest {
            bucket = bucketName
            key = keyName
        },
    )
}

internal suspend fun deleteMultiRegionAccessPoint(
    s3ControlClient: S3ControlClient,
    multiRegionAccessPointName: String,
    testAccountId: String,
) {
    val deleteRequestToken = s3ControlClient.deleteMultiRegionAccessPoint(
        DeleteMultiRegionAccessPointRequest {
            accountId = testAccountId
            details =
                DeleteMultiRegionAccessPointInput {
                    name = multiRegionAccessPointName
                }
        },
    )

    waitUntilMultiRegionAccessPointOperationCompletes(
        s3ControlClient,
        deleteRequestToken.requestTokenArn ?: throw Exception("Unable to get request token ARN"),
        1000 * 60 * 5, // 5 minutes
        testAccountId,
        "deleteMultiRegionAccessPoint",
    )
}

internal suspend fun waitUntilMultiRegionAccessPointOperationCompletes(
    s3ControlClient: S3ControlClient,
    request: String,
    timeLimit: Int,
    testAccountId: String,
    operation: String,
) {
    val startTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - startTime < timeLimit) {
        val status = s3ControlClient.describeMultiRegionAccessPointOperation(
            DescribeMultiRegionAccessPointOperationRequest {
                accountId = testAccountId
                requestTokenArn = request
            },
        ).asyncOperation?.requestStatus

        println("Waiting on $operation operation. Status: $status")
        if (status == "SUCCEEDED") return
        TimeUnit.SECONDS.sleep(10L) // Avoid constant status checks
    }

    throw Exception("The multi-region-access-point $operation operation exceeded the time limit set ($timeLimit ms)")
}

internal suspend fun deleteS3Bucket(
    s3Client: S3Client,
    bucketName: String,
) {
    s3Client.deleteBucket(
        DeleteBucketRequest {
            bucket = bucketName
        },
    )
}

internal suspend fun objectWasCreated(
    s3: S3Client,
    bucketName: String,
    keyName: String,
): Boolean {
    val search = s3.listObjectsV2(
        ListObjectsV2Request {
            bucket = bucketName
        },
    ).contents

    return search?.find { it.key == keyName } != null
}

internal suspend fun s3BucketWasCreated(
    s3: S3Client,
    bucketName: String,
): Boolean {
    val search = s3.listBuckets(
        ListBucketsRequest {},
    ).buckets

    return search?.find { it.name == bucketName } != null
}

internal suspend fun multiRegionAccessPointWasCreated(
    s3Control: S3ControlClient,
    multiRegionAccessPointName: String,
    testAccountId: String,
): Boolean {
    val search = s3Control.listMultiRegionAccessPoints(
        ListMultiRegionAccessPointsRequest {
            accountId = testAccountId
        },
    ).accessPoints?.find { it.name == multiRegionAccessPointName }

    return search != null
}

internal fun closeClients(
    vararg clients: SdkClient,
) {
    clients.forEach { client ->
        client.close()
    }
}
