/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth

import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.collections.AttributeKey

public object S3ExpressAttributes {
    public val Bucket: AttributeKey<String> = AttributeKey("aws.sdk.kotlin#S3ExpressBucket")
    public val Client: AttributeKey<SdkClient> = AttributeKey("aws.sdk.kotlin#S3ExpressClient")
}