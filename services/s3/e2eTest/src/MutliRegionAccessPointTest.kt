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
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme
import kotlinx.coroutines.runBlocking
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
            s3SigV4a,
            s3Control,
        )
    }
}

internal suspend fun cleanUpMrapTest(
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
