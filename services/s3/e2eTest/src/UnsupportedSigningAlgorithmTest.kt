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
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import aws.smithy.kotlin.runtime.auth.awssigning.UnsupportedSigningAlgorithmException
import kotlin.test.assertFailsWith
import io.kotest.matchers.string.shouldContain

class UnsupportedSigningAlgorithmTest {
//    @Test
    fun testUnsupportedSigningAlgorithm(): Unit = runBlocking {
        try {
            S3Client {
                region = "us-west-2"
            }.use { s3West ->

                createS3Bucket(
                    s3West,
                    usWestBucket,
                    BucketLocationConstraint.UsWest2,
                )

                s3West.withConfig {
                    region = "us-east-2"
                }.use { s3East ->

                    createS3Bucket(
                        s3East,
                        usEastBucket,
                        BucketLocationConstraint.UsEast2,
                    )

                    S3ControlClient {
                        region = "us-west-2"
                    }.use { s3Control ->

                        createMultiRegionAccessPoint(s3Control)
                        val multiRegionAccessPointArn = getMultiRegionAccessPointArn(s3Control) ?: throw Exception("Unable to get multi region access point arn")

                        assertFailsWith<UnsupportedSigningAlgorithmException> {
                            createObject(
                                s3West,
                                multiRegionAccessPointArn,
                            )
                        }.message.shouldContain("SIGV4A support is not yet implemented for the default signer.")

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
