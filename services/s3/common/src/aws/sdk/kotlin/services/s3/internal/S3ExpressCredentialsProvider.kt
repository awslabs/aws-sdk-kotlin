/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.auth.S3ExpressAttributes
import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.CreateSessionRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.util.ExpiringValue
import kotlin.coroutines.coroutineContext
import aws.smithy.kotlin.runtime.io.SdkManagedBase

public class S3ExpressCredentialsProvider(
    public val bootstrapCredentialsProvider: CredentialsProvider,
) : CloseableCredentialsProvider, SdkManagedBase() {
    private val credentialsCache = S3ExpressCredentialsCache()

    override suspend fun resolve(attributes: Attributes): Credentials {
        val logger = coroutineContext.logger<S3ExpressCredentialsProvider>()

        val bucket: String = attributes[S3ExpressAttributes.Bucket]
        val client = attributes[S3ExpressAttributes.Client]

        val key = S3ExpressCredentialsCacheKey(bucket, client, bootstrapCredentialsProvider.resolve(attributes))

        return credentialsCache.get(key).also { logger.trace { "Got credentials $it from cache" }}
    }

    override fun close() {
        credentialsCache.close()
    }
}
