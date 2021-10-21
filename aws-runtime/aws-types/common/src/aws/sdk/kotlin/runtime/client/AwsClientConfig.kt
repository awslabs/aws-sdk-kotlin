/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.client

import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider

/**
 * Shared AWS service client configuration that all AWS service clients implement as part of their configuration state.
 */
public interface AwsClientConfig {
    /**
     * The AWS region to make requests to
     */
    public val region: String

    /**
     * The [CredentialsProvider] that will be called to resolve credentials before making AWS service calls
     */
    public val credentialsProvider: CredentialsProvider

    public companion object {}
}
