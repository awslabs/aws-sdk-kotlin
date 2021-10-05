/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.sdk.kotlin.runtime.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadActiveAwsProfile
import aws.sdk.kotlin.runtime.endpoint.Endpoint
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
import aws.sdk.kotlin.runtime.resolve
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.util.PlatformProvider

internal const val EC2_METADATA_SERVICE_ENDPOINT_PROFILE_KEY = "ec2_metadata_service_endpoint"
internal const val EC2_METADATA_SERVICE_ENDPOINT_MODE_PROFILE_KEY = "ec2_metadata_service_endpoint_mode"

internal class ImdsEndpointResolver(
    private val platformProvider: PlatformProvider,
    private val endpointModeOverride: EndpointMode? = null,
    private val endpointOverride: Endpoint? = null,
) : EndpointResolver {
    // cached endpoint and profile
    private var resolvedEndpoint: Endpoint? = null
    private var cachedProfile: AwsProfile? = null

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

    private fun loadEndpointFromEnv(): Endpoint? =
        AwsSdkSetting.AwsEc2MetadataServiceEndpoint.resolve(platformProvider)?.toEndpoint()

    private suspend fun loadEndpointFromProfile(): Endpoint? {
        val profile = cachedProfileOrLoad()
        return profile[EC2_METADATA_SERVICE_ENDPOINT_PROFILE_KEY]?.toEndpoint()
    }

    private fun loadEndpointModeFromEnv(): EndpointMode? =
        AwsSdkSetting.AwsEc2MetadataServiceEndpointMode.resolve(platformProvider)?.let { EndpointMode.fromValue(it) }

    private suspend fun loadEndpointModeFromProfile(): EndpointMode? {
        val profile = cachedProfileOrLoad()
        return profile[EC2_METADATA_SERVICE_ENDPOINT_MODE_PROFILE_KEY]?.let { EndpointMode.fromValue(it) }
    }

    private suspend fun cachedProfileOrLoad(): AwsProfile = cachedProfile ?: loadActiveAwsProfile(platformProvider).also { cachedProfile = it }
}

// Parse a string as a URL and convert to an endpoint
internal fun String.toEndpoint(): Endpoint {
    val url = Url.parse(this)
    return Endpoint(url.host, url.scheme.protocolName, url.port)
}
