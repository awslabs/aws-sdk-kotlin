/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.client.SdkClientConfig
import aws.smithy.kotlin.runtime.client.region.RegionProvider

/**
 * Base interface all generated AWS SDK Kotlin clients implement
 */
public interface AwsSdkClientConfig : SdkClientConfig {

    /**
     * The AWS region (e.g. `us-west-2`) to make requests to. See about AWS
     * [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more information.
     * When specified, this static region configuration takes precedence over other region resolution methods.
     *
     * The region resolution order is:
     * 1. Static region (if specified)
     * 2. Custom region provider (if configured)
     * 3. Default region provider chain
     */
    public val region: String?

    /**
     * An optional region provider that determines the AWS region for client operations. When specified, this provider
     * takes precedence over the default region provider chain, unless a static region is explicitly configured.
     *
     * The region resolution order is:
     * 1. Static region (if specified)
     * 2. Custom region provider (if configured)
     * 3. Default region provider chain
     */
    public val regionProvider: RegionProvider

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

    /**
     * An optional application specific identifier.
     * When set it will be appended to the User-Agent header of every request in the form of: `app/{applicationId}`.
     * When not explicitly set, the value will be loaded from the following locations:
     *
     * - JVM System Property: `aws.userAgentAppId`
     * - Environment variable: `AWS_SDK_UA_APP_ID`
     * - Shared configuration profile attribute: `sdk_ua_app_id`
     *
     * See [shared configuration settings](https://docs.aws.amazon.com/sdkref/latest/guide/settings-reference.html)
     * reference for more information on environment variables and shared config settings.
     */
    public val applicationId: String?

    public interface Builder {
        /**
         * The AWS region (e.g. `us-west-2`) to make requests to. See about AWS
         * [global infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/) for more information.
         * When specified, this static region configuration takes precedence over other region resolution methods.
         *
         * The region resolution order is:
         * 1. Static region (if specified)
         * 2. Custom region provider (if configured)
         * 3. Default region provider chain
         */
        public var region: String?

        /**
         * An optional region provider that determines the AWS region for client operations. When specified, this provider
         * takes precedence over the default region provider chain, unless a static region is explicitly configured.
         *
         * The region resolution order is:
         * 1. Static region (if specified)
         * 2. Custom region provider (if configured)
         * 3. Default region provider chain
         */
        public var regionProvider: RegionProvider?

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

        /**
         * An optional application specific identifier.
         * When set it will be appended to the User-Agent header of every request in the form of: `app/{applicationId}`.
         * When not explicitly set, the value will be loaded from the following locations:
         *
         * - JVM System Property: `aws.userAgentAppId`
         * - Environment variable: `AWS_SDK_UA_APP_ID`
         * - Shared configuration profile attribute: `sdk_ua_app_id`
         *
         * See [shared configuration settings](https://docs.aws.amazon.com/sdkref/latest/guide/settings-reference.html)
         * reference for more information on environment variables and shared config settings.
         */
        public var applicationId: String?
    }
}
