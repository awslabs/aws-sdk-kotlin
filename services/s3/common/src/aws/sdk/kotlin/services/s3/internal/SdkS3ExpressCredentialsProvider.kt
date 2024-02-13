/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.S3Attributes
import aws.sdk.kotlin.services.s3.S3ExpressCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.io.SdkManagedBase

internal class DefaultS3ExpressCredentialsProvider(
    val bootstrapCredentialsProvider: CredentialsProvider,
) : S3ExpressCredentialsProvider, SdkManagedBase() {
    private val credentialsCache = S3ExpressCredentialsCache()

    override suspend fun resolve(attributes: Attributes): Credentials {
        val bucket = attributes[S3Attributes.DirectoryBucket]
        val client = attributes[S3Attributes.ExpressClient]

        val key = S3ExpressCredentialsCacheKey(bucket, client, bootstrapCredentialsProvider.resolve(attributes))

        return credentialsCache.get(key)
    }

    override fun close() {
        credentialsCache.close()
    }
}
