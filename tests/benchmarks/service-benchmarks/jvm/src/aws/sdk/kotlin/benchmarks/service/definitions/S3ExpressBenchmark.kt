/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.*
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.content.ByteStream

/**
 * Benchmarks for S3 Express One Zone.
 * Note: This benchmark must be run from an EC2 host in the same AZ as the bucket (usw2-az1).
 */
class S3ExpressBenchmark : ServiceBenchmark<S3Client> {
    private val regionAz = "usw2-az1"
    private val bucketName = Common.random("sdk-benchmark-bucket-")
        .substring(0 until 47) + // truncate to prevent "bucket name too long" errors
        "--$regionAz--x-s3"

    private val KEY = "64kb-object"
    private val CONTENTS = "a".repeat(65536) // 64KB

    @OptIn(ExperimentalApi::class)
    override suspend fun client() = S3Client.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override suspend fun setup(client: S3Client) {
        client.createBucket {
            bucket = bucketName
            createBucketConfiguration {
                location {
                    type = LocationType.AvailabilityZone
                    this.name = regionAz
                }
                bucket {
                    type = BucketType.Directory
                    dataRedundancy = DataRedundancy.SingleAvailabilityZone
                }
            }
        }
    }

    override val operations get() = listOf(putObjectBenchmark, getObjectBenchmark)

    override suspend fun tearDown(client: S3Client) {
        client.deleteBucket { bucket = bucketName }
    }

    private val putObjectBenchmark = object : AbstractOperationBenchmark<S3Client>("s3express:PutObject") {
        override suspend fun transact(client: S3Client) {
            client.putObject {
                bucket = bucketName
                key = KEY
                body = ByteStream.fromString(CONTENTS)
            }
        }

        override suspend fun tearDown(client: S3Client) {
            client.deleteObject {
                bucket = bucketName
                key = KEY
            }
        }
    }

    private val getObjectBenchmark = object : AbstractOperationBenchmark<S3Client>("s3express:GetObject") {
        override suspend fun setup(client: S3Client) {
            client.putObject {
                bucket = bucketName
                key = KEY
                body = ByteStream.fromString(CONTENTS)
            }
        }

        override suspend fun transact(client: S3Client) {
            client.getObject(
                GetObjectRequest {
                    bucket = bucketName
                    key = KEY
                },
            ) { }
        }

        override suspend fun tearDown(client: S3Client) {
            client.deleteObject {
                bucket = bucketName
                key = KEY
            }
        }
    }
}
