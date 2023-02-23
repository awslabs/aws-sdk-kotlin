/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.flexiblechecksums

import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape

/**
 * Adds a middleware which validates checksums returned in responses if the user has opted-in.
 */
class FlexibleChecksumsResponse : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) = model
        .shapes<OperationShape>()
        .any { it.hasTrait<HttpChecksumTrait>() }

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + flexibleChecksumsResponseMiddleware

    private val flexibleChecksumsResponseMiddleware = object : ProtocolMiddleware {
        override val name: String = "FlexibleChecksumsResponse"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
            val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()
            val input = op.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }

            return (httpChecksumTrait != null) &&
                (httpChecksumTrait.requestValidationModeMember?.getOrNull() != null) &&
                (input?.memberNames?.any { it == httpChecksumTrait.requestValidationModeMember.get() } == true)
        }

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val inputSymbol = ctx.symbolProvider.toSymbol(ctx.model.expectShape(op.inputShape))
            val interceptorSymbol = RuntimeTypes.HttpClient.Interceptors.FlexibleChecksumsResponseInterceptor

            val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()!!
            val requestValidationModeMember = ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .first { it.memberName == httpChecksumTrait.requestValidationModeMember.get() }

            writer.withBlock(
                "op.interceptors.add(#T<#T> {",
                "})",
                interceptorSymbol,
                inputSymbol,
            ) {
                writer.write("it.#L?.value == \"ENABLED\"", requestValidationModeMember.defaultName())
            }
        }
    }
}
