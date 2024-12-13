/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.flexiblechecksums

import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape

/**
 * Adds a middleware that enables sending flexible checksums during an HTTP request
 */
class FlexibleChecksumsRequest : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) = model
        .shapes<OperationShape>()
        .any { it.hasTrait<HttpChecksumTrait>() }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        listOf(
            ConfigProperty {
                name = "requestChecksumCalculation"
                symbol = RuntimeTypes.SmithyClient.Config.HttpChecksumConfigOption
                baseClass = RuntimeTypes.SmithyClient.Config.HttpChecksumClientConfig
                useNestedBuilderBaseClass()
                documentation = "Configures request checksum calculation"
                propertyType = ConfigPropertyType.RequiredWithDefault("HttpChecksumConfigOption.WHEN_SUPPORTED")
            },
        )

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + flexibleChecksumsRequestMiddleware + configBusinessMetrics

    private val configBusinessMetrics = object : ProtocolMiddleware {
        override val name: String = "requestChecksumCalculationBusinessMetric"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            op.hasTrait<HttpChecksumTrait>()

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.withBlock("when(config.requestChecksumCalculation) {", "}") {
                writer.write(
                    "#T.WHEN_SUPPORTED -> op.context.#T(#T.FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED)",
                    RuntimeTypes.SmithyClient.Config.HttpChecksumConfigOption,
                    RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                    RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
                )
                writer.write(
                    "#T.WHEN_REQUIRED -> op.context.#T(#T.FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED)",
                    RuntimeTypes.SmithyClient.Config.HttpChecksumConfigOption,
                    RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                    RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
                )
            }
        }
    }

    private val flexibleChecksumsRequestMiddleware = object : ProtocolMiddleware {
        override val name: String = "flexibleChecksumsRequestMiddleware"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
            val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()
            val input = op.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }

            return (httpChecksumTrait != null) &&
                (httpChecksumTrait.requestAlgorithmMember?.getOrNull() != null) &&
                (input?.memberNames?.any { it == httpChecksumTrait.requestAlgorithmMember.get() } == true)
        }

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val inputSymbol = ctx.symbolProvider.toSymbol(ctx.model.expectShape(op.inputShape))

            val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()!!

            val requestAlgorithmMember = ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .first { it.memberName == httpChecksumTrait.requestAlgorithmMember.get() }

            val requestAlgorithmMemberName = ctx.symbolProvider.toMemberName(requestAlgorithmMember)
            val requestChecksumRequired = httpChecksumTrait.isRequestChecksumRequired

            writer.withBlock(
                "op.interceptors.add(#T<#T>(",
                "))",
                RuntimeTypes.HttpClient.Interceptors.FlexibleChecksumsRequestInterceptor,
                inputSymbol,
            ) {
                writer.write("#L,", requestChecksumRequired)
                writer.write("config.requestChecksumCalculation,")
                writer.write("input.#L?.value,", requestAlgorithmMemberName)
            }
        }
    }
}
