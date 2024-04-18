/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.model.shapes
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.DeprecatedTrait
import software.amazon.smithy.model.traits.DocumentationTrait
import software.amazon.smithy.model.traits.HttpHeaderTrait
import software.amazon.smithy.model.transform.ModelTransformer
import kotlin.streams.asSequence

/**
 * An integration used to customize behavior around S3's output members named `Expires`.
 */
class S3ExpiresIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3 && model.shapes<OperationShape>().any { it.hasExpiresMember(model) }

    // Ensure `Expires` is a deprecated timestamp type and add a new synthetic member `ExpiresString`
    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()

        // Ensure all targets of `Expires` shapes are timestamps
        val expiresMemberShapes = model.shapes()
            .asSequence()
            .mapNotNull { shape ->
                shape.members().firstOrNull { member -> member.memberName.lowercase() == "expires" }
            }
            .map { it.target }
            .associateWith { ShapeType.TIMESTAMP }

        var transformedModel = transformer.changeShapeType(model, expiresMemberShapes)

        // Add a base `ExpiresString` shape
        val expiresString = StringShape.builder()
        expiresString.id("aws.sdk.kotlin.s3.synthetic#ExpiresString")
        transformedModel = transformedModel.toBuilder().addShape(expiresString.build()).build()

        // Deprecate `Expires` and add a synthetic member that targets `ExpiresString`
        return transformer.mapShapes(transformedModel) { shape ->
            if (shape.memberNames.any { it.lowercase() == "expires" }) {
                check(shape is StructureShape) { "expected shape ${shape.id} with `Expires` member to be a StructureShape" }
                val builder = shape.toBuilder()

                // Deprecate `Expires`
                val expiresMember = shape.members().first { it.memberName.lowercase() == "expires" }
                builder.removeMember(expiresMember.memberName)
                val deprecatedTrait = DeprecatedTrait.builder()
                    .message("Please use `expiresString` which contains the raw, unparsed value of this field.")
                    .since("2024-04-16")
                    .build()

                builder.addMember(
                    expiresMember.toBuilder()
                        .addTrait(deprecatedTrait)
                    .build()
                )

                // Add a synthetic member targeting `ExpiresString`
                val expiresStringMember = MemberShape.builder()
                expiresStringMember.target(expiresString.id)
                expiresStringMember.id(expiresMember.id.toString() + "String") // i.e. com.amazonaws.s3.<MEMBER_NAME>$ExpiresString
                expiresStringMember.addTrait(HttpHeaderTrait("Expires")) // Add HttpHeaderTrait to ensure the field is deserialized
                expiresStringMember.addTrait(expiresMember.getTrait<DocumentationTrait>()) // Copy documentation from `Expires`
                builder.addMember(expiresStringMember.build())
                builder.build()
            } else shape
        }
    }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> = resolved + applyExpiresFieldInterceptor

    private val applyExpiresFieldInterceptor = object : ProtocolMiddleware {
        override val name: String = "ExpiresFieldInterceptor"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            ctx.model.expectShape<ServiceShape>(ctx.settings.service).isS3 && op.hasExpiresMember(ctx.model)

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val interceptorSymbol = buildSymbol {
                name = "ExpiresFieldInterceptor"
                namespace = ctx.settings.pkg.subpackage("internal")
            }

            writer.write("op.interceptors.add(#T)", interceptorSymbol)
        }
    }

    private fun OperationShape.hasExpiresMember(model: Model): Boolean {
        val input = model.expectShape(this.inputShape)
        val output = model.expectShape(this.outputShape)

        return (input.memberNames + output.memberNames).any {
            it.lowercase() == "expires"
        }
    }
}
