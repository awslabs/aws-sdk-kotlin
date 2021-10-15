/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

/**
 * Represents a set of AWS credentials
 *
 * For more information see [AWS security credentials](https://docs.aws.amazon.com/general/latest/gr/aws-security-credentials.html#AccessKeys)
 */
public data class Credentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String? = null
)
