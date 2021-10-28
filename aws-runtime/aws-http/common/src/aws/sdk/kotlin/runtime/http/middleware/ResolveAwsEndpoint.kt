/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.middleware.setRequestEndpoint
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.http.operation.getLogger
import aws.smithy.kotlin.runtime.util.get

/**
 * Http feature for resolving the (AWS) service endpoint.
 */
@InternalSdkApi
public class ResolveAwsEndpoint(
    config: Config
) : Feature {

    private val serviceId: String = requireNotNull(config.serviceId) { "ServiceId must not be null" }
    private val resolver: AwsEndpointResolver = requireNotNull(config.resolver) { "EndpointResolver must not be null" }

    public class Config {
        /**
         * The AWS service ID to resolve endpoints for
         */
        public var serviceId: String? = null

        /**
         * The resolver to use
         */
        public var resolver: AwsEndpointResolver? = null
    }

    public companion object Feature : HttpClientFeatureFactory<Config, ResolveAwsEndpoint> {
        override val key: FeatureKey<ResolveAwsEndpoint> = FeatureKey("ServiceEndpointResolver")

        override fun create(block: Config.() -> Unit): ResolveAwsEndpoint {
            val config = Config().apply(block)
            return ResolveAwsEndpoint(config)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.mutate.intercept { req, next ->

            val region = req.context[AwsClientOption.Region]
            val endpoint = resolver.resolve(serviceId, region)
            setRequestEndpoint(req, endpoint.endpoint)

            endpoint.credentialScope?.let { scope ->
                // resolved endpoint has credential scope override(s), update the context for downstream consumers
                scope.service?.let {
                    if (it.isNotBlank()) req.context[AuthAttributes.SigningService] = it
                }
                scope.region?.let {
                    if (it.isNotBlank()) req.context[AuthAttributes.SigningRegion] = it
                }
            }

            val logger = req.context.getLogger("ResolveAwsEndpoint")
            logger.debug { "resolved endpoint: $endpoint" }

            next.call(req)
        }
    }
}
