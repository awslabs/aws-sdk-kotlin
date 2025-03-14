/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.endpoints

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.SmithyBusinessMetric

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

@InternalApi
public fun AccountIdEndpointMode.toBusinessMetric(): BusinessMetric = when (this) {
    AccountIdEndpointMode.PREFERRED -> SmithyBusinessMetric.ACCOUNT_ID_MODE_PREFERRED
    AccountIdEndpointMode.DISABLED -> SmithyBusinessMetric.ACCOUNT_ID_MODE_DISABLED
    AccountIdEndpointMode.REQUIRED -> SmithyBusinessMetric.ACCOUNT_ID_MODE_REQUIRED
    else -> throw IllegalStateException("Unexpected AccountIdEndpointMode value: ${this::class.simpleName}")
}
