/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.buildSymbol
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.namespace

/**
 * HTTP client interceptor that resolves service endpoints for a single service
 */
class EndpointResolverFeature(private val ctx: ProtocolGenerator.GenerationContext) : HttpFeature {
    override val name: String = "ServiceEndpointResolver"
    override fun addImportsAndDependencies(writer: KotlinWriter) {
        val resolverFeatureSymbol = buildSymbol {
            name = "ServiceEndpointResolver"
            namespace(AwsKotlinDependency.AWS_CLIENT_RT_HTTP)
        }

        // generated symbol
        val defaultResolverSymbol = buildSymbol {
            name = "DefaultEndpointResolver"
            namespace = "${ctx.settings.moduleName}.internal"
        }

        writer.addImport(resolverFeatureSymbol)
        writer.addImport(defaultResolverSymbol)
    }

    override fun renderConfigure(writer: KotlinWriter) {
        writer.write("serviceId = ServiceId")
        writer.write("resolver = DefaultEndpointResolver()")
    }
}
