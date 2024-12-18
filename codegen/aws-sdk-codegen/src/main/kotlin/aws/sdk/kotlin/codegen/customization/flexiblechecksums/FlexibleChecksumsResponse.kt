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
 * Handles flexible checksum responses
 */
class FlexibleChecksumsResponse : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.isTraitApplied(HttpChecksumTrait::class.java)

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        listOf(
            // Allows flexible checksum response configuration
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
        resolved + flexibleChecksumsResponseMiddleware + responseChecksumValidationBusinessMetric
}

/**
 * Emits business metric based on `responseChecksumValidation` client config
 */
private val responseChecksumValidationBusinessMetric = object : ProtocolMiddleware {
    override val name: String = "responseChecksumValidationBusinessMetric"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.withBlock("when(config.responseChecksumValidation) {", "}") {
            // Supported
            writer.write(
                "#T.WHEN_SUPPORTED -> op.context.#T(#T.FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED)",
                RuntimeTypes.SmithyClient.Config.HttpChecksumConfigOption,
                RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
            )
            // Required
            writer.write(
                "#T.WHEN_REQUIRED -> op.context.#T(#T.FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED)",
                RuntimeTypes.SmithyClient.Config.HttpChecksumConfigOption,
                RuntimeTypes.Core.BusinessMetrics.emitBusinessMetric,
                RuntimeTypes.Core.BusinessMetrics.SmithyBusinessMetric,
            )
        }
    }
}

/**
 * Adds interceptor to handle flexible checksum response validation
 */
private val flexibleChecksumsResponseMiddleware = object : ProtocolMiddleware {
    override val name: String = "flexibleChecksumsResponseMiddleware"

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
        val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()
        val inputShape = op.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }

        return (httpChecksumTrait != null) &&
            (httpChecksumTrait.requestValidationModeMember?.getOrNull() != null) &&
            (inputShape?.memberNames?.any { it == httpChecksumTrait.requestValidationModeMember.get() } == true)
    }

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val inputSymbol = ctx.symbolProvider.toSymbol(ctx.model.expectShape(op.inputShape))
        val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()!!
        val requestValidationModeMember = ctx.model.expectShape<StructureShape>(op.input.get())
            .members()
            .first { it.memberName == httpChecksumTrait.requestValidationModeMember.get() }
        val requestValidationModeMemberName = ctx.symbolProvider.toMemberName(requestValidationModeMember)

        writer.withBlock( // TODO: ADD DIFFERENT INTERCEPTORS DEPENDING ON IF THE SERVICE HAS COMPOSITE CHECKSUMS !
            "op.interceptors.add(#T<#T>(",
            "))",
            RuntimeTypes.HttpClient.Interceptors.FlexibleChecksumsResponseInterceptor,
            inputSymbol,
        ) {
            writer.write("input.#L?.value == \"ENABLED\",", requestValidationModeMemberName)
            writer.write("config.responseChecksumValidation,")
        }
    }
}
