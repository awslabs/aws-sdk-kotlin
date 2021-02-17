/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
import software.aws.clientrt.http.*
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.util.get

/**
 *  Http feature for resolving the service endpoint.
 */
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
        operation.execution.state.intercept { req, next ->

            val region = req.context[AwsClientOption.Region]
            val endpoint = resolver.resolve(serviceId, region)
            req.request.url.scheme = Protocol.parse(endpoint.protocol)
            req.request.url.host = endpoint.hostname
            req.request.headers["Host"] = endpoint.hostname

            next.call(req)
        }
    }
}
