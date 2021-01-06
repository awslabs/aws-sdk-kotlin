/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.buildSymbol
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.namespace

// TODO - placeholder for generating per/service endpoint data from endpoints.json

/**
 * Generates a per/service endpoint resolver (internal to the generated SDK) using endpoints.json
 */
class EndpointResolverGenerator {

    fun render(ctx: ProtocolGenerator.GenerationContext) {
        ctx.delegator.useFileWriter("DefaultEndpointResolver.kt", "${ctx.settings.moduleName}.internal") {
            renderResolver(ctx, it)
        }
    }

    private fun renderResolver(ctx: ProtocolGenerator.GenerationContext, writer: KotlinWriter) {
        val endpointResolverSymbol = buildSymbol {
            name = "EndpointResolver"
            namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE, "endpoint")
        }
        writer.addImport(endpointResolverSymbol)
        val endpointSymbol = buildSymbol {
            name = "Endpoint"
            namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE, "endpoint")
        }
        writer.addImport(endpointSymbol)

        writer.openBlock("internal class DefaultEndpointResolver : EndpointResolver {", "}") {
            writer.openBlock("override suspend fun resolve(service: String, region: String): Endpoint {", "}") {
                // TODO - parse endpoints.json and generate internal types to resolve with
                val hostname = "${ctx.service.endpointPrefix}.\${region}.amazonaws.com"
                writer.write("return Endpoint(\$S, \$S)", hostname, "https")
            }
        }
    }
}
