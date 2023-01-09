/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.client

import aws.smithy.kotlin.runtime.client.SdkClientConfig

/**
 * Base interface all generated AWS SDK Kotlin clients implement
 */
public interface AwsSdkClientConfig : SdkClientConfig {

    /**
     * The AWS region to make requests to
     */
    public val region: String

    public interface Builder<TConfig : SdkClientConfig> : SdkClientConfig.Builder<TConfig> {

        /**
         * Configure the AWS region (e.g. `us-west-2`) to make requests to. See about AWS
         * [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more
         * information
         */
        public var region: String?
    }
}
