/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.endpoint

import aws.smithy.kotlin.runtime.http.endpoints.AwsEndpoint
import aws.smithy.kotlin.runtime.http.endpoints.AwsEndpointResolver

/**
 * An [AwsEndpointResolver] that returns a static endpoint
 */
public class StaticEndpointResolver(private val endpoint: AwsEndpoint) : AwsEndpointResolver {
    override suspend fun resolve(service: String, region: String): AwsEndpoint = endpoint
}
