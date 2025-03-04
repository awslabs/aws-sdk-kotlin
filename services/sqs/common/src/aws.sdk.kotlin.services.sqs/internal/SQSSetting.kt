/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.sqs.internal

import aws.smithy.kotlin.runtime.config.*

/**
 * SQS specific system settings
 */
internal object SQSSetting {
    /**
     * Configure when MD5 checksum validation is performed for SQS operations.
     *
     * Can be configured using:
     * - System property: aws.SqsChecksumValidationEnabled
     * - Environment variable: AWS_SQS_CHECKSUM_VALIDATION_ENABLED
     *
     * Valid values:
     * - ALWAYS (default) - Validates checksums for both sending and receiving operations
     * - WHEN_SENDING - Validates checksums only when sending messages
     * - WHEN_RECEIVING - Validates checksums only when receiving messages
     * - NEVER - Disables checksum validation
     *
     * Note: Value matching is case-insensitive when configured via environment variables.
     */
    public val checksumValidationEnabled: EnvironmentSetting<ValidationEnabled> =
        enumEnvSetting("aws.SqsChecksumValidationEnabled", "AWS_SQS_CHECKSUM_VALIDATION_ENABLED")

    /**
     * Configure the scope of checksum validation for SQS operations.
     *
     * Can be configured using:
     * - System property: aws.SqsChecksumValidationScope
     * - Environment variable: AWS_SQS_CHECKSUM_VALIDATION_SCOPE
     *
     * Valid values are comma-separated combinations of:
     * - MESSAGE_BODY: Validate message body checksums
     * - MESSAGE_ATTRIBUTES: Validate message attribute checksums
     * - SYSTEM_ATTRIBUTES: Validate system attribute checksums
     *
     * Example: "MESSAGE_BODY,MESSAGE_ATTRIBUTES"
     *
     * If not specified, defaults to validating all scopes.
     */
    public val checksumValidationScopes: EnvironmentSetting<Set<ValidationScope>?> =
        enumSetEnvSetting<ValidationScope>("aws.SqsChecksumValidationScope", "AWS_SQS_CHECKSUM_VALIDATION_SCOPE")
}
