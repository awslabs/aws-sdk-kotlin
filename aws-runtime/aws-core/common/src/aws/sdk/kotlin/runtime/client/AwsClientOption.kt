/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.client

import aws.smithy.kotlin.runtime.client.ClientOption

/**
 * A collection of AWS service client options. NOTE: most options are configured by default through the service
 * config
 */
public object AwsClientOption {
    /**
     * The AWS region the client should use. Note this is not always the same as [AwsSigningAttributes.SigningRegion] in
     * the case of global services like IAM
     */
    public val Region: ClientOption<String> = ClientOption("AwsRegion")
}
