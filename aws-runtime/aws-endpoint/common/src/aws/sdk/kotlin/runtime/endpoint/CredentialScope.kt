/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.endpoint

/**
 * A custom signing constraint for an endpoint
 *
 * @property region A custom sigv4 signing name
 * @property service A custom sigv4 service name to use when signing a request
 */
public data class CredentialScope(val region: String? = null, val service: String? = null)
