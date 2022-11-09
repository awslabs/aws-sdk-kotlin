/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.middleware

import aws.sdk.kotlin.codegen.protocols.endpoints.bindAwsBuiltinsSymbol
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.getOperationInputOutputSymbols
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointParameterBindingGenerator
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.ResolveEndpointMiddlewareGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.rulesengine.language.EndpointRuleSet
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait

/**
 * HTTP client interceptor that resolves service endpoints for a single service
 */
class ResolveAwsEndpointMiddleware(private val ctx: ProtocolGenerator.GenerationContext) : ProtocolMiddleware {
    override val name: String = "ResolveAwsEndpoint"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        if (!ctx.service.hasTrait<EndpointRuleSetTrait>()) {
            writeDefault(writer)
            return
        }

        val rules = EndpointRuleSet.fromNode(ctx.service.expectTrait<EndpointRuleSetTrait>().ruleSet)
        val middlewareSymbol = ResolveEndpointMiddlewareGenerator.getSymbol(ctx.settings)
        val (inputSymbol, outputSymbol) = OperationIndex.of(ctx.model).getOperationInputOutputSymbols(op, ctx.symbolProvider)

        writer.withBlock(
            "op.install(#T<#T, #T>(config.endpointProvider) { input ->",
            "})",
            middlewareSymbol,
            inputSymbol,
            outputSymbol,
        ) {
            write("#T(config)", bindAwsBuiltinsSymbol(ctx.settings))
            EndpointParameterBindingGenerator(ctx.model, ctx.service, writer, op, rules, "input.").render()
        }
    }

    private fun writeDefault(writer: KotlinWriter) {
        writer.write("op.install(#T(config.endpointResolver))", RuntimeTypes.Http.Middlware.ResolveEndpoint)
    }
}
