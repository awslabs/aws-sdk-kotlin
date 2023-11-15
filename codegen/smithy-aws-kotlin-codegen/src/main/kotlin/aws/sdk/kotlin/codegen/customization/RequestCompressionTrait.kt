/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.shapes
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.traits.RequestCompressionTrait

class RequestCompressionTrait : KotlinIntegration {

    // Will determine if middleware is registered for service
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model
            .shapes<OperationShape>() // TODO: We should not directly iterate operation shapes like this, we always need to consult the service closure.
            .any { it.hasTrait<RequestCompressionTrait>() }

    // Registers middleware
    override fun customizeMiddleware(
            ctx: ProtocolGenerator.GenerationContext,
            resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> = resolved + requestCompressionTraitMiddleware

    // Register additional config
    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> {
        return super.additionalServiceConfigProps(ctx) + listOf(
                ConfigProperty {
                    name = "compressionAlgorithms"
                    documentation = """
                        The mutable list of compression algorithms supported by the SDK.
                        More compression algorithms can be added and may override an existing implementation.
                        Use the `compressionAlgorithm` interface to create one.
                    """.trimIndent()
                    symbol = Symbol.builder().build() // TODO: Write proper values for these
                    baseClass = Symbol.builder().build() // TODO: Write proper values for these
                    // TODO: Do a little research into what the rest of the possible properties do
                }
        )
    }

    // Middleware
    private val requestCompressionTraitMiddleware = object : ProtocolMiddleware {
        private val interceptorSymbol = RuntimeTypes.HttpClient.Interceptors.RequestCompressionTraitInterceptor
        override val name: String = "RequestCompressionTrait"

        // Will add interceptor to operation(s) in service with `requestCompression` trait
        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            op.getTrait<RequestCompressionTrait>()?.let { trait ->
                val requestedCompressionAlgorithms = trait.encodings

                writer.withBlock(
                        "if (config.disableRequestCompression == false) {",
                        "}"
                ) {
                    withBlock(
                            "op.interceptors.add(#T(",
                            "))",
                            interceptorSymbol,
                    ) {
                        write("config.requestMinCompressionSizeBytes,")
                        write("listOf(${requestedCompressionAlgorithms.joinToString(", ")}),")
                        write("config.compressionAlgorithms")
                    }
                }
            }
        }
    }
}