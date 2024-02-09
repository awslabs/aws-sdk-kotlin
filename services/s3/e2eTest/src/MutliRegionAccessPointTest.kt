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
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MutliRegionAccessPointTest {
    private val s3West = S3Client { region = "us-west-2" }
    private val s3East = s3West.withConfig { region = "us-east-2" }
    private val s3SigV4a = s3West.withConfig { authSchemes = listOf(SigV4AsymmetricAuthScheme(CrtAwsSigner)) }
    private val s3Control = S3ControlClient { region = "us-west-2" }

    private val usWestBucket = "${S3TestUtils.TEST_BUCKET_PREFIX}for-aws-kotlin-sdk-us-west-2"
    private val usEastBucket = "${S3TestUtils.TEST_BUCKET_PREFIX}for-aws-kotlin-sdk-us-east-2"
    private val multiRegionAccessPoint = "aws-sdk-for-kotlin-test-multi-region-access-point"
    private val keyForObject = "test.txt"

    @BeforeAll
    private fun setUpMrapTest(): Unit = runBlocking {
        println("Setting up MutliRegionAccessPointTest tests")

        val accountId = getAccountId()

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
    }

    @AfterAll
    private fun cleanUpMrapTest(): Unit = runBlocking {
        println("Cleaning up MutliRegionAccessPointTest tests")

        val accountId = getAccountId()

        if (multiRegionAccessPointWasCreated(s3Control, multiRegionAccessPoint, accountId)) {
            deleteMultiRegionAccessPoint(s3Control, multiRegionAccessPoint, accountId)
        }

        if (s3BucketWasCreated(s3West, usWestBucket)) {
            deleteS3Bucket(s3West, usWestBucket)
        }

        if (s3BucketWasCreated(s3East, usEastBucket)) {
            if (objectWasCreated(s3East, usEastBucket, keyForObject)) {
                deleteObject(s3East, usEastBucket, keyForObject)
            }
            deleteS3Bucket(s3East, usEastBucket)
        }

        closeClients(
            s3West,
            s3East,
            s3SigV4a,
            s3Control,
        )
    }

    @Test
    fun testMultRegionAccessPointOperation(): Unit = runBlocking {
        try {
            val accountId = getAccountId()

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
        } catch (exception: Throwable) {
            println("Test failed with exception: ${exception.cause}")
            throw exception
        }
    }

    @Test
    fun testUnsupportedSigningAlgorithm(): Unit = runBlocking {
        try {
            val accountId = getAccountId()

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
            }.message.shouldContain("SIGV4_ASYMMETRIC support is not yet implemented for the default signer.")
        } catch (exception: Throwable) {
            println("Test failed with exception: ${exception.cause}")
            throw exception
        }
    }
}
