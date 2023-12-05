/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.compression

import aws.smithy.kotlin.runtime.config.EnvironmentSetting
import aws.smithy.kotlin.runtime.config.boolEnvSetting
import aws.smithy.kotlin.runtime.config.longEnvSetting

public object CompressionSettings {
    /**
     * Determines if a request should be compressed or not
     */
    public val AwsDisableRequestCompression: EnvironmentSetting<Boolean> =
        boolEnvSetting("aws.disableRequestCompression", "AWS_DISABLE_REQUEST_COMPRESSION")

    /**
     * The threshold used to determine if a request should be compressed
     */
    public val AwsRequestMinCompressionSizeBytes: EnvironmentSetting<Long> =
        longEnvSetting("aws.requestMinCompressionSizeBytes", "AWS_REQUEST_MIN_COMPRESSION_SIZE_BYTES")
}
