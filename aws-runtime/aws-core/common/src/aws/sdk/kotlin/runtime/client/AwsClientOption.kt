/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.client

import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * A collection of AWS service client options. NOTE: most options are configured by default through the service
 * config
 */
public object AwsClientOption {
    /**
     * The AWS region the client should use. Note this is not always the same as [AwsSigningAttributes.SigningRegion] in
     * the case of global services like IAM.
     *
     * NOTE: Synonymous with [aws.smithy.kotlin.runtime.awsprotocol.AwsAttributes.Region]
     */
    public val Region: AttributeKey<String> = AttributeKey("aws.smithy.kotlin#AwsRegion")

    /**
     * The ID of the AWS account requests are routed to.
     */
    public val AccountId: AttributeKey<String> = AttributeKey("aws.sdk.kotlin#AccountId")
}
