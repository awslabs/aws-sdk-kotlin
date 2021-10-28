/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.operation.HttpOperationContext
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.util.get

/**
 *  Http feature for resolving the service endpoint.
 */
@InternalSdkApi
public class ServiceEndpointResolver(
    config: Config
) : Feature {

    private val serviceId: String = requireNotNull(config.serviceId) { "ServiceId must not be null" }
    private val resolver: EndpointResolver = requireNotNull(config.resolver) { "EndpointResolver must not be null" }

    public class Config {
        /**
         * The AWS service ID to resolve endpoints for
         */
        public var serviceId: String? = null

        /**
         * The resolver to use
         */
        public var resolver: EndpointResolver? = null
    }

    public companion object Feature : HttpClientFeatureFactory<Config, ServiceEndpointResolver> {
        override val key: FeatureKey<ServiceEndpointResolver> = FeatureKey("ServiceEndpointResolver")

        override fun create(block: Config.() -> Unit): ServiceEndpointResolver {
            val config = Config().apply(block)
            return ServiceEndpointResolver(config)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.mutate.intercept { req, next ->

            val region = req.context[AwsClientOption.Region]
            val endpoint = resolver.resolve(serviceId, region)
            req.subject.url.scheme = Protocol.parse(endpoint.protocol)
            if (req.subject.url.host == "") {
                val hostPrefix = req.context.getOrNull(HttpOperationContext.HostPrefix) ?: ""
                val hostname = "${hostPrefix}${endpoint.hostname}"
                req.subject.url.host = hostname
                req.subject.url.port = endpoint.port
                req.subject.headers["Host"] = hostname
            }

            endpoint.signingName?.let {
                if (it.isNotBlank()) req.context[AuthAttributes.SigningService] = it
            }
            endpoint.signingRegion?.let {
                if (it.isNotBlank()) req.context[AuthAttributes.SigningRegion] = it
            }

            next.call(req)
        }
    }
}
