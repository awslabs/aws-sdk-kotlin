/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.requestcompression

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.defaultValue
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.traits.RequestCompressionTrait

class RequestCompressionTrait : KotlinIntegration {

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean = model
        .serviceShapes.any { serviceShape ->
            serviceShape.operations.any { operation ->
                model.expectShape<OperationShape>(operation).hasTrait<RequestCompressionTrait>()
            }
        }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + requestCompressionTraitMiddleware

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        super.additionalServiceConfigProps(ctx) + listOf(
            ConfigProperty {
                name = "compressionAlgorithms"
                documentation = """
                    The mutable list of compression algorithms supported by the SDK.
                    More compression algorithms can be added and may override an existing implementation.
                    Use the `compressionAlgorithm` interface to create one.
                """.trimIndent()
                symbol = Symbol.builder() // TODO: Not sure if this imports the right thing ... look at examples
                    .name("List<CompressionAlgorithm>")
                    .defaultValue("listOf(Gzip())")
                    .build()
            },
        )
}

private val requestCompressionTraitMiddleware = object : ProtocolMiddleware {
    private val interceptorSymbol = RuntimeTypes.HttpClient.Interceptors.RequestCompressionTraitInterceptor
    override val name: String = "RequestCompressionTrait"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        op.getTrait<RequestCompressionTrait>()?.let { trait ->
            val requestedCompressionAlgorithms = trait.encodings

            writer.withBlock(
                "if (config.disableRequestCompression == false) {",
                "}",
            ) {
                withBlock(
                    "op.interceptors.add(#T(",
                    "))",
                    interceptorSymbol,
                ) {
                    write("config.requestMinCompressionSizeBytes,")
                    write(
                        "listOf(${requestedCompressionAlgorithms.joinToString(
                            separator = ", ",
                            transform = {
                                "\"$it\""
                            },
                        )}),",
                    )
                    write("config.compressionAlgorithms")
                }
            }
        }
    }
}
