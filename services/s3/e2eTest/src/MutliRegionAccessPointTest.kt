/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.e2etest.S3TestUtils.createMultiRegionAccessPoint
import aws.sdk.kotlin.e2etest.S3TestUtils.deleteBucketAndAllContents
import aws.sdk.kotlin.e2etest.S3TestUtils.deleteMultiRegionAccessPoint
import aws.sdk.kotlin.e2etest.S3TestUtils.getAccountId
import aws.sdk.kotlin.e2etest.S3TestUtils.getBucket
import aws.sdk.kotlin.e2etest.S3TestUtils.getMultiRegionAccessPointArn
import aws.sdk.kotlin.e2etest.S3TestUtils.multiRegionAccessPointWasCreated
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.putObject
import aws.sdk.kotlin.services.s3.withConfig
import aws.sdk.kotlin.services.s3control.S3ControlClient
import aws.smithy.kotlin.runtime.auth.awssigning.UnsupportedSigningAlgorithmException
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val MRAP_BUCKET_PREFIX = "s3-mrap-test-bucket-"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MutliRegionAccessPointTest {
    private val s3West = S3Client { region = "us-west-2" }
    private val s3East = s3West.withConfig { region = "us-east-2" }
    private val s3SigV4a = s3West.withConfig { authSchemes = listOf(SigV4AsymmetricAuthScheme(CrtAwsSigner)) }
    private val s3Control = S3ControlClient { region = "us-west-2" }

    private val multiRegionAccessPoint = "aws-sdk-for-kotlin-test-multi-region-access-point"
    private val objectKey = "test.txt"

    private lateinit var accountId: String
    private lateinit var multiRegionAccessPointArn: String
    private lateinit var usWestBucket: String
    private lateinit var usEastBucket: String

    @BeforeAll
    private fun setUp(): Unit = runBlocking {
        accountId = getAccountId()
        usWestBucket = getBucket(s3West, MRAP_BUCKET_PREFIX, "us-west-2", accountId)
        usEastBucket = getBucket(s3East, MRAP_BUCKET_PREFIX, "us-east-2", accountId)

        createMultiRegionAccessPoint(
            s3Control,
            multiRegionAccessPoint,
            usWestBucket,
            usEastBucket,
            accountId,
        )

        multiRegionAccessPointArn =
            getMultiRegionAccessPointArn(
                s3Control,
                multiRegionAccessPoint,
                accountId,
            )
    }

    @AfterAll
    private fun cleanUp(): Unit = runBlocking {
        if (multiRegionAccessPointWasCreated(s3Control, multiRegionAccessPoint, accountId)) {
            deleteMultiRegionAccessPoint(s3Control, multiRegionAccessPoint, accountId)
        }

        deleteBucketAndAllContents(s3West, usWestBucket)
        deleteBucketAndAllContents(s3East, usEastBucket)

        s3West.close()
        s3East.close()
        s3SigV4a.close()
        s3Control.close()
    }

    @Test
    fun testMultiRegionAccessPointOperation(): Unit = runBlocking {
        s3SigV4a.putObject {
            bucket = multiRegionAccessPointArn
            key = objectKey
        }

        s3SigV4a.deleteObject {
            bucket = multiRegionAccessPointArn
            key = objectKey
        }
    }

    @Test
    fun testUnsupportedSigningAlgorithm(): Unit = runBlocking {
        val ex = assertFailsWith<UnsupportedSigningAlgorithmException> {
            s3West.putObject {
                bucket = multiRegionAccessPointArn
                key = objectKey
            }
        }

        assertEquals(
            ex.message,
            "SIGV4A support is not yet implemented for the default signer. For more information on how to enable it with the CRT signer, please refer to: https://a.co/3sf8533",
        )
    }
}
