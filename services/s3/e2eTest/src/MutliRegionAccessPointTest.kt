/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.CreateBucketConfiguration
import aws.sdk.kotlin.services.s3.model.CreateBucketRequest
import aws.sdk.kotlin.services.s3.model.DeleteBucketRequest
import aws.sdk.kotlin.services.s3.withConfig
import aws.sdk.kotlin.services.s3control.S3ControlClient
import aws.sdk.kotlin.services.s3control.model.*
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class MutliRegionAccessPointTest {
    @Test
    fun testMultRegionAccessPointOperation(): Unit = runBlocking {
        val accountId = getAccountId()
        val s3West = S3Client {
            region = "us-west-2"
        }
        val s3East = s3West.withConfig {
            region = "us-east-2"
        }
        val s3SigV4a = s3West.withConfig {
            authSchemes = listOf(SigV4AsymmetricAuthScheme(CrtAwsSigner))
        }
        val s3Control = S3ControlClient {
            region = "us-west-2"
        }

        val usWestBucket = "aws-sdk-for-kotlin-test-bucket-us-west-2"
        val usEastBucket = "aws-sdk-for-kotlin-test-bucket-us-east-2"
        val multiRegionAccessPoint = "aws-sdk-for-kotlin-test-multi-region-access-point"
        val keyForObject = "test.txt"

        try {
            createS3Bucket(
                s3West,
                usWestBucket,
                BucketLocationConstraint.UsWest2,
            )

            createS3Bucket(
                s3East,
                usEastBucket,
                BucketLocationConstraint.UsEast2,
            )

            createMultiRegionAccessPoint(
                s3Control,
                multiRegionAccessPoint,
                usWestBucket,
                usEastBucket,
                accountId,
            )

            val multiRegionAccessPointArn =
                getMultiRegionAccessPointArn(
                    s3Control,
                    multiRegionAccessPoint,
                    accountId,
                )

            createObject(
                s3SigV4a,
                multiRegionAccessPointArn,
                keyForObject,
            )

            deleteObject(
                s3SigV4a,
                multiRegionAccessPointArn,
                keyForObject,
            )

            deleteMultiRegionAccessPoint(
                s3Control,
                multiRegionAccessPoint,
                accountId,
            )

            deleteS3Bucket(
                s3West,
                usWestBucket,
            )

            deleteS3Bucket(
                s3East,
                usEastBucket,
            )
        } catch (exception: Throwable) {
            closeClients(
                s3West,
                s3East,
                s3SigV4a,
                s3Control,
            )
            cleanUpTest(
                accountId,
                multiRegionAccessPoint,
                usWestBucket,
                usEastBucket,
                keyForObject,
            )
            throw exception
        }
        closeClients(
            s3West,
            s3East,
            s3SigV4a,
            s3Control,
        )
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

private suspend fun waitUntilMultiRegionAccessPointOperationCompletes(
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

internal suspend fun cleanUpTest(
    testAccountId: String,
    multiRegionAccessPointName: String,
    usWestBucket: String,
    usEastBucket: String,
    keyName: String,
) {
    S3ControlClient {
        region = "us-west-2"
    }.use { s3Control ->
        if (multiRegionAccessPointWasCreated(s3Control, multiRegionAccessPointName, testAccountId)) deleteMultiRegionAccessPoint(s3Control, multiRegionAccessPointName, testAccountId)
    }

    S3Client {
        region = "us-west-2"
    }.use { s3West ->
        if (s3BucketWasCreated(s3West, usWestBucket)) deleteS3Bucket(s3West, usWestBucket)

        s3West.withConfig {
            region = "us-east-2"
        }.use { s3East ->
            if (s3BucketWasCreated(s3East, usEastBucket)) {
                if (objectWasCreated(s3East, usEastBucket, keyName)) deleteObject(s3East, usEastBucket, keyName)
                deleteS3Bucket(s3East, usEastBucket)
            }
        }
    }
}

private suspend fun objectWasCreated(
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

private suspend fun s3BucketWasCreated(
    s3: S3Client,
    bucketName: String,
): Boolean {
    val search = s3.listBuckets(
        ListBucketsRequest {},
    ).buckets

    return search?.find { it.name == bucketName } != null
}

private suspend fun multiRegionAccessPointWasCreated(
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
