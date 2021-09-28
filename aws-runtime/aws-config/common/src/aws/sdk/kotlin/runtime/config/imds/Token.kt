/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.smithy.kotlin.runtime.time.Instant

/**
 * Tokens are cached to remove the need to reload the token between subsequent requests. To ensure
 * a request never fails with a 401 (expired token), a buffer window exists during which the token
 * is not expired but refreshed anyway to ensure the token doesn't expire during an in-flight operation.
 */
internal const val TOKEN_REFRESH_BUFFER_SECONDS = 120

internal const val X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS = "x-aws-ec2-metadata-token-ttl-seconds"
internal const val X_AWS_EC2_METADATA_TOKEN = "x-aws-ec2-metadata-token"

internal data class Token(val value: ByteArray, val expires: Instant) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Token

        if (!value.contentEquals(other.value)) return false
        if (expires != other.expires) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.contentHashCode()
        result = 31 * result + expires.hashCode()
        return result
    }
}
