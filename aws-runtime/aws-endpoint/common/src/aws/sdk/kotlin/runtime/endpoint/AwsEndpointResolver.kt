/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.endpoint

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awssigning.SigningContext
import aws.smithy.kotlin.runtime.auth.awssigning.SigningContextualizedEndpoint
import aws.smithy.kotlin.runtime.auth.awssigning.SigningEndpointProvider

/**
 * Resolves endpoints for a given service and region
 */
public fun interface AwsEndpointResolver {

    /**
     * Resolve the [AwsEndpoint] for the given service and region
     * @param service the service id associated with the desired endpoint
     * @param region the region associated with the desired endpoint
     * @return an [AwsEndpoint] that can be used to connect to the service
     */
    public suspend fun resolve(service: String, region: String): AwsEndpoint
}

@InternalSdkApi
public fun AwsEndpointResolver.asSigningEndpointProvider(): SigningEndpointProvider = {
    val awsEndpoint = resolve(requireNotNull(it.service), requireNotNull(it.region))
    SigningContextualizedEndpoint(awsEndpoint.endpoint, awsEndpoint.credentialScope?.asSigningContext())
}

private fun CredentialScope.asSigningContext() = SigningContext(service, region)
