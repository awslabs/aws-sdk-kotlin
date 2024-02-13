/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3

import aws.sdk.kotlin.services.s3.internal.DefaultS3ExpressCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CloseableCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes

/**
 * A credentials provider used for making requests to S3 Express One Zone directory buckets.
 */
public interface S3ExpressCredentialsProvider : CloseableCredentialsProvider {
    override suspend fun resolve(attributes: Attributes): Credentials
    override fun close()

    public companion object {
        public fun default(bootstrapCredentialsProvider: CredentialsProvider): S3ExpressCredentialsProvider =
            DefaultS3ExpressCredentialsProvider(bootstrapCredentialsProvider)
    }
}
