/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

/**
 * Represents a set of AWS credentials
 */
public data class Credentials(val accessKeyId: String, val secretAccessKey: String, val sessionToken: String? = null)
