/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.runtime.auth.credentials.ProcessCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.CreateBucketConfiguration
import aws.sdk.kotlin.services.s3.model.CreateBucketRequest
import aws.sdk.kotlin.services.s3.model.DeleteBucketRequest
import aws.sdk.kotlin.services.s3.withConfig
import aws.sdk.kotlin.services.s3control.S3ControlClient
import aws.sdk.kotlin.services.s3control.model.*
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.test.Test

private const val usWestBucket = "aws-sdk-for-kotlin-test-bucket-west"
private const val usEastBucket = "aws-sdk-for-kotlin-test-bucket-east"
private const val multiRegionAccessPoint = "aws-sdk-for-kotlin-test-multi-region-access-point"
private const val testAccountId = "393681621311"
private const val testKeyForObject = "text.txt"

class MutliRegionAccessPointTest {
    @Test
    fun testMultRegionAccessPoints(): Unit = runBlocking {
        try {
            S3Client {
                region = "us-west-2"
                credentialsProvider = ProcessCredentialsProvider("isengardcli credentials --awscli aoperez@amazon.com --role Admin") // TODO: Remove
            }.use { s3West ->

                createS3Bucket(
                    s3West,
                    usWestBucket,
                    BucketLocationConstraint.UsWest2
                )

                s3West.withConfig {
                    region = "us-east-2"
                }.use { s3East ->

                    createS3Bucket(
                        s3East,
                        usEastBucket,
                        BucketLocationConstraint.UsEast2
                    )

                    S3ControlClient {
                        region = "us-west-2"
                        credentialsProvider = ProcessCredentialsProvider("isengardcli credentials --awscli aoperez@amazon.com --role Admin") // TODO: Remove
                    }.use { s3Control ->

                        createMultiRegionAccessPoint(s3Control)
                        val multiRegionAccessPointArn = getMultiRegionAccessPointArn(s3Control) ?: throw Exception("Unable to get multi region access point arn")

                        s3West.withConfig {
                            authSchemes = listOf(SigV4AsymmetricAuthScheme(CrtAwsSigner))
                        }.use { s3SigV4a ->

                            createObject(
                                s3SigV4a,
                                multiRegionAccessPointArn,
                            )

                            deleteObject(
                                s3SigV4a,
                                multiRegionAccessPointArn,
                            )
                        }

                        deleteMultiRegionAccessPoint(s3Control)

                        deleteS3Bucket(
                            s3West,
                            usWestBucket,
                        )

                        deleteS3Bucket(
                            s3East,
                            usEastBucket,
                        )
                    }
                }
            }
        } catch (exception: Throwable) {
            cleanUpTest()
            throw exception
        }
    }
}

private suspend fun createS3Bucket(
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
        }
    )
}

private suspend fun createMultiRegionAccessPoint(
    s3ControlClient: S3ControlClient,
) {
    val createRequestToken = s3ControlClient.createMultiRegionAccessPoint(
        CreateMultiRegionAccessPointRequest {
            accountId = testAccountId
            details =
                CreateMultiRegionAccessPointInput {
                    name = multiRegionAccessPoint
                    regions =
                        listOf(
                            Region {
                                bucket = usWestBucket
                            },
                            Region {
                                bucket = usEastBucket
                            }
                        )
                }
        }
    )

    waitUntilMultiRegionAccessPointOperationCompletes(
        s3ControlClient,
        createRequestToken.requestTokenArn ?: throw Exception("Unable to get request token ARN"),
        1000 * 60 * 10, // 10 minutes
    )
}

private suspend fun getMultiRegionAccessPointArn(
    s3ControlClient: S3ControlClient,
): String? =
    s3ControlClient.listMultiRegionAccessPoints(
        ListMultiRegionAccessPointsRequest {
            accountId = testAccountId
        }
    ).accessPoints?.find { it.name == multiRegionAccessPoint }?.alias?.let { alias ->
        return "arn:aws:s3::$testAccountId:accesspoint/$alias"
    }

private suspend fun createObject(
    s3Client: S3Client,
    bucketName: String,
) {
    s3Client.putObject(
        PutObjectRequest {
            bucket = bucketName
            key = testKeyForObject
        }
    )
}

private suspend fun deleteObject(
    s3Client: S3Client,
    bucketName: String,
) {
    s3Client.deleteObject(
        DeleteObjectRequest {
            bucket = bucketName
            key = testKeyForObject
        }
    )
}

private suspend fun deleteMultiRegionAccessPoint(
    s3ControlClient: S3ControlClient,
) {
    val deleteRequestToken = s3ControlClient.deleteMultiRegionAccessPoint(
        DeleteMultiRegionAccessPointRequest {
            accountId = testAccountId
            details =
                DeleteMultiRegionAccessPointInput {
                    name = multiRegionAccessPoint
                }
        }
    )

    waitUntilMultiRegionAccessPointOperationCompletes(
        s3ControlClient,
        deleteRequestToken.requestTokenArn ?: throw Exception("Unable to get request token ARN"),
        1000 * 60 * 5, // 5 minutes
    )
}

private suspend fun waitUntilMultiRegionAccessPointOperationCompletes(
    s3ControlClient: S3ControlClient,
    request: String,
    timeLimit: Int,
) {
    val startTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - startTime < timeLimit) {
        val status = s3ControlClient.describeMultiRegionAccessPointOperation(
            DescribeMultiRegionAccessPointOperationRequest {
                accountId = testAccountId
                requestTokenArn = request
            }
        ).asyncOperation?.requestStatus

        println(status) // TODO: Remove
        if (status == "SUCCEEDED") return
        TimeUnit.SECONDS.sleep(10L)
    }

    throw Exception("The multi-region-access-point operation exceeded the time limit set ($timeLimit ms)")
}

private suspend fun deleteS3Bucket(
    s3Client: S3Client,
    bucketName: String,
) {
    s3Client.deleteBucket(
        DeleteBucketRequest {
            bucket = bucketName
        }
    )
}

private suspend fun cleanUpTest() {
    S3ControlClient {
        region = "us-west-2"
        credentialsProvider = ProcessCredentialsProvider("isengardcli credentials --awscli aoperez@amazon.com --role Admin") // TODO: Remove
    }. use { s3Control ->
        if (multiRegionAccessPointWasCreated(s3Control)) deleteMultiRegionAccessPoint(s3Control)
    }

    S3Client {
        region = "us-west-2"
        credentialsProvider = ProcessCredentialsProvider("isengardcli credentials --awscli aoperez@amazon.com --role Admin") // TODO: Remove
    }.use { s3West ->
        if (s3BucketWasCreated(s3West, usWestBucket)) deleteS3Bucket(s3West, usWestBucket)

        s3West.withConfig {
            region = "us-east-2"
        }.use { s3East ->
            if (s3BucketWasCreated(s3East, usEastBucket)) {
                if (objectWasCreated(s3East, usEastBucket)) deleteObject(s3East, usEastBucket)
                deleteS3Bucket(s3East, usEastBucket)
            }
        }
    }
}

private suspend fun objectWasCreated(
    s3: S3Client,
    bucketName: String,
) : Boolean {
    val search = s3.listObjectsV2(
        ListObjectsV2Request {
            bucket = bucketName
        }
    ).contents

    return search?.find { it.key == testKeyForObject } != null
}

private suspend fun s3BucketWasCreated(
    s3: S3Client,
    bucketName: String,
): Boolean {
    val search = s3.listBuckets(
        ListBucketsRequest {}
    ).buckets

    return search?.find { it.name == bucketName } != null
}

private suspend fun multiRegionAccessPointWasCreated(
    s3Control: S3ControlClient,
): Boolean {
    val search = s3Control.listMultiRegionAccessPoints(
        ListMultiRegionAccessPointsRequest {
            accountId = testAccountId
        }
    ).accessPoints?.find { it.name == multiRegionAccessPoint }

    return search != null
}
