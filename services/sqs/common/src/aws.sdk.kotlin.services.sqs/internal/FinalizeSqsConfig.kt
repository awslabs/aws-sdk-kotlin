/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.sqs.internal

import aws.sdk.kotlin.runtime.config.profile.*
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

internal suspend fun finalizeSqsConfig(
    builder: SqsClient.Builder,
    sharedConfig: LazyAsyncValue<AwsSharedConfig>,
    provider: PlatformProvider = PlatformProvider.System,
) {
    val activeProfile = sharedConfig.get().activeProfile
    builder.config.checksumValidationEnabled = builder.config.checksumValidationEnabled
        ?: SqsSetting.checksumValidationEnabled.resolve(provider)
        ?: activeProfile.checksumValidationEnabled
        ?: ValidationEnabled.NEVER // TODO: MD5 checksum validation is temporarily disabled. Set default to ALWAYS in next minor version

    builder.config.checksumValidationScopes = builder.config.checksumValidationScopes.ifEmpty {
        SqsSetting.checksumValidationScopes.resolve(provider)
            ?: activeProfile.checksumValidationScopes
            ?: ValidationScope.entries.toSet()
    }
}

private val AwsProfile.checksumValidationEnabled: ValidationEnabled?
    get() = getEnumOrNull<ValidationEnabled>("sqs_checksum_validation_enabled")

private val AwsProfile.checksumValidationScopes: Set<ValidationScope>?
    get() = getEnumSetOrNull<ValidationScope>("sqs_checksum_validation_scope")
