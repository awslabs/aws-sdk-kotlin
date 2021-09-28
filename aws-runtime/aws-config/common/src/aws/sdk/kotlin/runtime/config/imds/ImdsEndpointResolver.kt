/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.sdk.kotlin.runtime.AwsSdkSetting
import aws.sdk.kotlin.runtime.endpoint.Endpoint
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
import aws.sdk.kotlin.runtime.resolve
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.util.Platform

internal class ImdsEndpointResolver(
    private val endpointModeOverride: EndpointMode? = null,
    private val endpointOverride: Endpoint? = null
) : EndpointResolver {
    // cached endpoint
    private var resolvedEndpoint: Endpoint? = null

    override suspend fun resolve(service: String, region: String): Endpoint = resolvedEndpoint ?: doResolveEndpoint()

    private suspend fun doResolveEndpoint(): Endpoint {
        val resolved = endpointOverride ?: resolveEndpointFromConfig()
        return resolved.also { resolvedEndpoint = it }
    }

    private suspend fun resolveEndpointFromConfig(): Endpoint {
        // explicit endpoint configured
        val endpoint = loadEndpointFromEnv() ?: loadEndpointFromProfile()
        if (endpoint != null) return endpoint

        // endpoint default from mode
        val endpointMode = endpointModeOverride ?: loadEndpointModeFromEnv() ?: loadEndpointModeFromProfile() ?: EndpointMode.IPv4
        return endpointMode.defaultEndpoint
    }

    private suspend fun loadEndpointFromEnv(): Endpoint? {
        val uri = AwsSdkSetting.AwsEc2MetadataServiceEndpoint.resolve(Platform) ?: return null
        return Url.parse(uri).let { Endpoint(it.host, it.scheme.protocolName) }
    }

    private suspend fun loadEndpointFromProfile(): Endpoint? = null

    private suspend fun loadEndpointModeFromEnv(): EndpointMode? =
        AwsSdkSetting.AwsEc2MetadataServiceEndpointMode.resolve(Platform)?.let { EndpointMode.fromValue(it) }

    private suspend fun loadEndpointModeFromProfile(): EndpointMode? = null
}
