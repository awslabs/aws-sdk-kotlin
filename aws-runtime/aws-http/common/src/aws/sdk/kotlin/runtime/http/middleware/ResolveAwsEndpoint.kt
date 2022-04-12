/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAttributes
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.endpoints.AwsEndpointResolver
import aws.smithy.kotlin.runtime.http.middleware.setRequestEndpoint
import aws.smithy.kotlin.runtime.http.operation.ModifyRequestMiddleware
import aws.smithy.kotlin.runtime.http.operation.SdkHttpRequest
import aws.smithy.kotlin.runtime.http.operation.getLogger
import aws.smithy.kotlin.runtime.util.get

/**
 * Http feature for resolving the (AWS) service endpoint.
 */
@InternalSdkApi
public class ResolveAwsEndpoint(
    /**
     * The AWS service ID to resolve endpoints for
     */
    private val serviceId: String,

    /**
     * The resolver to use
     */
    private val resolver: AwsEndpointResolver

) : ModifyRequestMiddleware {

    override suspend fun modifyRequest(req: SdkHttpRequest): SdkHttpRequest {
        val region = req.context[AwsClientOption.Region]
        val endpoint = resolver.resolve(serviceId, region)
        setRequestEndpoint(req, endpoint.endpoint)

        endpoint.credentialScope?.let { scope ->
            // resolved endpoint has credential scope override(s), update the context for downstream consumers
            scope.service?.let {
                if (it.isNotBlank()) req.context[AwsSigningAttributes.SigningService] = it
            }
            scope.region?.let {
                if (it.isNotBlank()) req.context[AwsSigningAttributes.SigningRegion] = it
            }
        }

        val logger = req.context.getLogger("ResolveAwsEndpoint")
        logger.trace { "resolved endpoint: $endpoint" }
        return req
    }
}
