/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.flexiblechecksums

import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
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
 * Adds a middleware which validates checksums returned in responses if the user has opted-in.
 */
class FlexibleChecksumsResponse : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) = model
        .shapes<OperationShape>()
        .any { it.hasTrait<HttpChecksumTrait>() }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        listOf(
            ConfigProperty {
                name = "responseChecksumValidation"
                symbol = RuntimeTypes.SmithyClient.Config.HttpChecksumConfigOption
                baseClass = RuntimeTypes.SmithyClient.Config.HttpChecksumClientConfig
                useNestedBuilderBaseClass()
                documentation = "Configures response checksum validation"
                propertyType = ConfigPropertyType.RequiredWithDefault("HttpChecksumConfigOption.WHEN_SUPPORTED")
            },
        )

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + flexibleChecksumsResponseMiddleware + configBusinessMetrics

    private val configBusinessMetrics = object : ProtocolMiddleware {
        override val name: String = "responseChecksumValidationBusinessMetric"

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            writer.withBlock("when(config.responseChecksumValidation) {", "}") {
                writer.write(
                    "#T.WHEN_SUPPORTED -> op.context.#T(#T.FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED)",
                    RuntimeTypes.SmithyClient.Config.HttpChecksumConfigOption,
                    RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                    RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
                )
                writer.write(
                    "#T.WHEN_REQUIRED -> op.context.#T(#T.FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED)",
                    RuntimeTypes.SmithyClient.Config.HttpChecksumConfigOption,
                    RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                    RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
                )
            }
        }
    }

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
            val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()!!
            val requestValidationModeMember = ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .first { it.memberName == httpChecksumTrait.requestValidationModeMember.get() }
            val requestValidationModeMemberName = ctx.symbolProvider.toMemberName(requestValidationModeMember)

            writer.withBlock(
                "op.interceptors.add(#T(",
                "))",
                RuntimeTypes.HttpClient.Interceptors.FlexibleChecksumsResponseInterceptor,
            ) {
                writer.write("responseValidationRequired = input.#L?.value == \"ENABLED\",", requestValidationModeMemberName)
                writer.write("responseChecksumValidation = config.responseChecksumValidation,")
            }
        }
    }
}
