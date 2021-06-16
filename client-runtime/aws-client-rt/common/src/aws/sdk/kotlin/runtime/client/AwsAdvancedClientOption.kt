/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.client

import aws.smithy.kotlin.runtime.client.ClientOption

/**
 * A collection of advanced options that can be configured on an AWS service client
 */
public object AwsAdvancedClientOption {
    /**
     * Whether region detection should be enabled. Region detection is used when the region is not specified
     * when building a client. This is enabled by default.
     */
    public val EnableDefaultRegionDetection: ClientOption<Boolean> = ClientOption("EnableDefaultRegionDetection")
}
