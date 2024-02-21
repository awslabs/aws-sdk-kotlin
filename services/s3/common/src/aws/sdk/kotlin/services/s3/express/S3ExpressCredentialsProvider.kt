/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider

/**
 * A credentials provider used for making requests to S3 Express One Zone directory buckets.
 */
public interface S3ExpressCredentialsProvider : CredentialsProvider {
    public companion object {
        /**
         * Create an instance of the default [S3ExpressCredentialsProvider] implementation
         */
        public fun default(): S3ExpressCredentialsProvider = DefaultS3ExpressCredentialsProvider()
    }
}
