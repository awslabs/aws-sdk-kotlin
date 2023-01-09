/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.client

import aws.smithy.kotlin.runtime.client.SdkClient

/**
 * Common interface all AWS SDK Kotlin generated service clients implement
 */
public interface AwsSdkClient : SdkClient {

    public interface Builder<
        TConfig : AwsSdkClientConfig,
        TConfigBuilder : AwsSdkClientConfig.Builder<TConfig>,
        TClient : AwsSdkClient,
        > : SdkClient.Builder<TConfig, TConfigBuilder, TClient>
}
