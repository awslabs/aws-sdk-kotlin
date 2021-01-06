/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.endpoint

/**
 * Represents the endpoint a service client should make API operation calls to.
 *
 * The SDK will automatically resolve these endpoints per API client using
 * an internal resolver.
 *
 * @property hostname The base URL endpoint clients will use to make API calls to e.g. "{service-id}.{region}.amazonaws.com"
 * @property protocol The protocol to use when making a connection e.g. "HTTPS"
 */
public data class Endpoint(
    public val hostname: String,
    public val protocol: String
)
