/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.withConfig
import aws.sdk.kotlin.services.s3control.S3ControlClient
import aws.sdk.kotlin.services.s3control.model.*
import aws.smithy.kotlin.runtime.auth.awssigning.UnsupportedSigningAlgorithmException
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class UnsupportedSigningAlgorithmTest {
    @Test
    fun testUnsupportedSigningAlgorithm(): Unit = runBlocking {
        val accountId = getAccountId()
        val s3West = S3Client {
            region = "us-west-2"
        }
        val s3East = s3West.withConfig {
            region = "us-east-2"
        }
        val s3Control = S3ControlClient {
            region = "us-west-2"
        }

        val usWestBucket = "${S3TestUtils.TEST_BUCKET_PREFIX}for-aws-kotlin-sdk-us-west-2"
        val usEastBucket = "${S3TestUtils.TEST_BUCKET_PREFIX}for-aws-kotlin-sdk-us-east-2"
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

            assertFailsWith<UnsupportedSigningAlgorithmException> {
                createObject(
                    s3West,
                    multiRegionAccessPointArn,
                    keyForObject,
                )
            }.message.shouldContain("SIGV4A support is not yet implemented for the default signer.")

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
                s3Control,
            )
            cleanUpMrapTest(
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
            s3Control,
        )
    }
}
