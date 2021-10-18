/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.client.AwsClientConfig

/**
 * Options that control how an [aws.sdk.kotlin.runtime.client.AwsClientConfig] is resolved
 */
public class AwsClientConfigLoadOptions {
    // TODO - see for an idea https://github.com/aws/aws-sdk-go-v2/blob/973e5f5e9477e0690bcb4be3145bb72e956651cc/config/load_options.go#L22
    // Most of these will not be needed if you are using a corresponding environment variable or system property but there should be an explicit
    // option to control behavior.
}

/**
 * Load the AWS client configuration from the environment.
 */
public suspend fun AwsClientConfig.Companion.loadFromEnvironment(
    block: AwsClientConfigLoadOptions.() -> Unit = {}
): AwsClientConfig = TODO()
