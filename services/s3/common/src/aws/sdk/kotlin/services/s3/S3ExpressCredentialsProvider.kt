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
    public companion object {
        /**
         * Create an instance of the default [S3ExpressCredentialsProvider] implementation
         * @param bootstrapCredentialsProvider the credentials provider used to call s3:CreateSession to retrieve S3 Express
         * credentials
         */
        public fun default(bootstrapCredentialsProvider: CredentialsProvider): S3ExpressCredentialsProvider =
            DefaultS3ExpressCredentialsProvider(bootstrapCredentialsProvider)
    }
}
