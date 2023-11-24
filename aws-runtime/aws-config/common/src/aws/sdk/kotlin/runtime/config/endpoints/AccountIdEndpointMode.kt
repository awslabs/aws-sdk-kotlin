/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.endpoints

/**
 * Controls how the account ID endpoint parameter is bound for services that support routing
 * endpoints based on it.
 */
public enum class AccountIdEndpointMode {
    /**
     * Endpoint parameters are populated on a best effort basis.
     */
    PREFERRED,

    /**
     * Endpoint parameters are never populated even when they are available.
     */
    DISABLED,

    /**
     * Endpoint parameters are always populated and an error is raised if the AWS account ID
     * is not available.
     */
    REQUIRED,
}
