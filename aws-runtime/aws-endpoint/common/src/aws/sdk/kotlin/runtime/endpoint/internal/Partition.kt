/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.endpoint.internal

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.sdk.kotlin.runtime.endpoint.CredentialScope
import aws.smithy.kotlin.runtime.http.Protocol
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.operation.Endpoint

private const val defaultProtocol = "https"
private const val defaultSigner = "v4"
private val protocolPriority = listOf("https", "http")
private val signerPriority = listOf("v4")

/**
 * A description of a single service endpoint
 */
@InternalSdkApi
public data class EndpointDefinition(
    /**
     * A URI **template** used to resolve the hostname of the endpoint.
     * Templates are of the form `{name}`. e.g. `{service}.{region}.amazonaws.com`
     *
     * Variables that may be substituted:
     * - `service` - the service name
     * - `region` - the region name
     * - `dnsSuffix` - the dns suffix of the partition
     */
    val hostname: String? = null,

    /**
     * A list of supported protocols for the endpoint (e.g. "https", "http", etc)
     */
    val protocols: List<String> = emptyList(),

    /**
     * A custom signing constraint for the endpoint
     */
    val credentialScope: CredentialScope? = null,

    /**
     * A list of allowable signature versions for the endpoint (e.g. "v4", "v2", "v3", "s3v3", etc)
     */
    val signatureVersions: List<String> = emptyList()
)

/**
 * A partition describes logical slice(s) of the AWS fabric
 */
@InternalSdkApi
public data class Partition(
    /**
     * The partition name/id e.g. "aws"
     */
    val id: String,

    /**
     * The regular expression that specified the pattern that region names in the endpoint adhere to
     */
    val regionRegex: Regex,

    /**
     * Endpoint that works across all regions or if [isRegionalized] is false
     */
    val partitionEndpoint: String,

    /**
     * Flag indicating whether or not the service is regionalized in the partition. Some services have only a single,
     * partition-global endpoint (e.g. CloudFront).
     */
    val isRegionalized: Boolean,

    /**
     * Default endpoint values for the partition. Some or all of the defaults specified may be superseded
     * by an entry in [endpoints].
     */
    val defaults: EndpointDefinition,

    /**
     * Map of endpoint names to their definitions
     */
    val endpoints: Map<String, EndpointDefinition>
)

// test if this partition is able to resolve an endpoint for the given region
internal fun Partition.canResolveEndpoint(region: String): Boolean =
    endpoints.containsKey(region) || regionRegex.matches(region)

internal fun Partition.resolveEndpoint(region: String): AwsEndpoint {
    val resolvedRegion = if (region.isEmpty() && partitionEndpoint.isNotEmpty()) {
        partitionEndpoint
    } else {
        region
    }

    val endpointDefinition = endpointDefinitionForRegion(resolvedRegion)
    return endpointDefinition.resolve(region, defaults)
}

private fun Partition.endpointDefinitionForRegion(region: String): EndpointDefinition {
    val match = when {
        endpoints.containsKey(region) -> endpoints[region]
        !isRegionalized -> endpoints[partitionEndpoint]
        else -> null
    }

    // return a matching definition or create an empty one that will be used for
    // generic endpoint resolution (e.g. un-modeled endpoints)
    return match ?: EndpointDefinition()
}

internal fun EndpointDefinition.resolve(region: String, defaults: EndpointDefinition): AwsEndpoint {
    val merged = mergeDefinitions(this, defaults)

    // hostname must have been set either by default or on this endpoint
    checkNotNull(merged.hostname) { "EndpointDefinition.hostname cannot be null at this point" }

    val hostname = merged.hostname.replace("{region}", region)
    val protocol = getByPriority(merged.protocols, protocolPriority, defaultProtocol)
    val signingName = merged.credentialScope?.service
    val signingRegion = merged.credentialScope?.region ?: region

    val uri = Url(Protocol.parse(protocol), hostname)
    val scope = CredentialScope(signingRegion, signingName)
    return AwsEndpoint(Endpoint(uri), scope)
}

/**
 * Create a copy of [into] endpoint definition by merging in values from [other]. Values already set
 * with a value on [into] are favored
 */
private fun mergeDefinitions(into: EndpointDefinition, from: EndpointDefinition): EndpointDefinition {
    val hostname = into.hostname ?: from.hostname
    val protocols = if (into.protocols.isNotEmpty()) into.protocols else from.protocols
    val credentialScope = CredentialScope(
        region = into.credentialScope?.region ?: from.credentialScope?.region,
        service = into.credentialScope?.service ?: from.credentialScope?.service
    )
    val signatureVersions = if (into.signatureVersions.isNotEmpty()) into.signatureVersions else from.signatureVersions
    return EndpointDefinition(hostname, protocols, credentialScope, signatureVersions)
}

// find the highest priority value out of [from] given the prioritized list [priority]. Otherwise use the default
private fun getByPriority(from: List<String>, priority: List<String>, default: String): String {
    if (from.isEmpty()) return default

    for (p in priority) {
        val candidate = from.find { it == p }
        if (candidate != null) return candidate
    }

    return default
}

/**
 * Resolve an endpoint for a given region using the given partitions
 */
@InternalSdkApi
public fun resolveEndpoint(partitions: List<Partition>, region: String): AwsEndpoint? {
    if (partitions.isEmpty()) return null

    // fallback to first partition if no candidate found
    val candidate = partitions.firstOrNull { it.canResolveEndpoint(region) } ?: partitions[0]

    return candidate.resolveEndpoint(region)
}
