/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.retries.RetryPolicyInfo
import software.amazon.smithy.kotlin.codegen.retries.StandardRetryIntegration

/**
 * Adds AWS-specific retry wrappers around operation invocations. This reuses the [StandardRetryIntegration] but
 * replaces [StandardRetryPolicy][aws.smithy.kotlin.runtime.retries.impl] with
 * [AwsDefaultRetryPolicy][aws.sdk.kotlin.runtime.http.retries].
 */
class AwsDefaultRetryIntegration : StandardRetryIntegration() {
    override val replacesIntegrations: Set<Class<out KotlinIntegration>>
        get() = setOf(StandardRetryIntegration::class.java)

    override val retryPolicyInfo: RetryPolicyInfo
        get() = RetryPolicyInfo("AwsDefaultRetryPolicy", AwsRuntimeTypes.Http.Retries.AwsDefaultRetryPolicy)
}
