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
     * The AWS region (e.g. `us-west-2`) to make requests to. See about AWS
     * [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more
     * information
     */
    public val region: String?

    /**
     * Flag to toggle whether to use [FIPS](https://aws.amazon.com/compliance/fips/) endpoints when making requests.
     * Disabled by default.
     */
    public val useFips: Boolean

    /**
     * Flag to toggle whether to use dual-stack endpoints when making requests.
     * See [https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoints.html] for more information.
     * Disabled by default.
     */
    public val useDualStack: Boolean

    public interface Builder {
        /**
         * The AWS region (e.g. `us-west-2`) to make requests to. See about AWS
         * [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more
         * information
         */
        public var region: String?

        /**
         * Flag to toggle whether to use [FIPS](https://aws.amazon.com/compliance/fips/) endpoints when making requests.
         * Disabled by default.
         */
        public var useFips: Boolean?

        /**
         * Flag to toggle whether to use dual-stack endpoints when making requests.
         * See [https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoints.html] for more information.
         * Disabled by default.
         */
        public var useDualStack: Boolean?
    }
}
