/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.S3Attributes
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.S3ExpressCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.io.SdkManagedBase

internal class DefaultS3ExpressCredentialsProvider(
    val bootstrapCredentialsProvider: CredentialsProvider,
) : S3ExpressCredentialsProvider, SdkManagedBase() {
    private lateinit var credentialsCache: S3ExpressCredentialsCache

    override suspend fun resolve(attributes: Attributes): Credentials {
        if (!this::credentialsCache.isInitialized) {
            credentialsCache = S3ExpressCredentialsCache(attributes[S3Attributes.ExpressClient] as S3Client)
        }

        val key = S3ExpressCredentialsCacheKey(attributes[S3Attributes.DirectoryBucket], bootstrapCredentialsProvider.resolve(attributes))
        return credentialsCache.get(key)
    }

    override fun close() {
        credentialsCache.close()
    }
}
