/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Execution context attributes specific to S3
 */
@InternalSdkApi
public object S3Attributes {
    /**
     * The name of the bucket requests are being made to
     */
    public val Bucket: AttributeKey<String> = AttributeKey("aws.sdk.kotlin#S3Bucket")

    /**
     * The S3 client being used to make requests to directory buckets. This client will be used to call s3:CreateSession
     * to obtain directory bucket credentials.
     */
    public val ExpressClient: AttributeKey<SdkClient> = AttributeKey("aws.sdk.kotlin#S3ExpressClient")
}
