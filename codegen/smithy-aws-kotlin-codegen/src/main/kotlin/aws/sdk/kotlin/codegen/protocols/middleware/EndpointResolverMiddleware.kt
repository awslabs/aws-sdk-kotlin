/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpFeatureMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator

/**
 * HTTP client interceptor that resolves service endpoints for a single service
 */
class EndpointResolverMiddleware(private val ctx: ProtocolGenerator.GenerationContext) : HttpFeatureMiddleware() {
    override val name: String = "ServiceEndpointResolver"
    override fun renderConfigure(writer: KotlinWriter) {
        val resolverFeatureSymbol = buildSymbol {
            name = "ServiceEndpointResolver"
            namespace(AwsKotlinDependency.AWS_HTTP, subpackage = "middleware")
        }
        writer.addImport(resolverFeatureSymbol)

        writer.write("serviceId = ServiceId")
        writer.write("resolver = config.endpointResolver")
    }
}
