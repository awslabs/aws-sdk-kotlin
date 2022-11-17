/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.middleware

import aws.sdk.kotlin.codegen.protocols.endpoints.bindAwsBuiltinsSymbol
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.getEndpointRules
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointParameterBindingGenerator
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.ResolveEndpointMiddlewareGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * HTTP client interceptor that resolves service endpoints for a single service
 */
class ResolveAwsEndpointMiddleware(private val ctx: ProtocolGenerator.GenerationContext) : ProtocolMiddleware {
    override val name: String = "ResolveAwsEndpoint"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.withBlock(
            "op.install(#T(config.endpointProvider) {",
            "})",
            ResolveEndpointMiddlewareGenerator.getSymbol(ctx.settings),
        ) {
            write("#T(config)", bindAwsBuiltinsSymbol(ctx.settings))
            ctx.service.getEndpointRules()?.let { rules ->
                EndpointParameterBindingGenerator(ctx.model, ctx.service, writer, op, rules, "it.").render()
            }
        }
    }
}
