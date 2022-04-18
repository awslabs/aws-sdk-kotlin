/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.endpoint

/**
 * An [AwsEndpointResolver] that returns a static endpoint
 */
public class StaticEndpointResolver(private val endpoint: AwsEndpoint) : AwsEndpointResolver {
    override suspend fun resolve(service: String, region: String): AwsEndpoint = endpoint
}
