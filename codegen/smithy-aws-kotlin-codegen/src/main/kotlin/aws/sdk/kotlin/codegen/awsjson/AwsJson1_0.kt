/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.awsjson

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait
import software.amazon.smithy.kotlin.codegen.core.KotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.ext.getTrait
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.serde.descriptorName
import software.amazon.smithy.kotlin.codegen.rendering.serde.serialKind
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.JsonNameTrait
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Handles generating the aws.protocols#awsJson1_0 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class AwsJson1_0 : AwsHttpBindingProtocolGenerator() {

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val httpMiddleware = super.getDefaultHttpMiddleware(ctx)
        val awsJsonMiddleware = listOf(
            AwsJsonProtocolMiddleware("1.0"),
            AwsJsonModeledExceptionsMiddleware(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return httpMiddleware + awsJsonMiddleware
    }

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        AwsJsonHttpBindingResolver(ctx, "application/x-amz-json-1.0")

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override fun generateSdkFieldDescriptor(
        ctx: ProtocolGenerator.GenerationContext,
        memberShape: MemberShape,
        writer: KotlinWriter,
        memberTargetShape: Shape?,
        namePostfix: String
    ) = JsonSerdeFieldGenerator.generateSdkFieldDescriptor(ctx, memberShape, writer, memberTargetShape, namePostfix)

    override fun generateSdkObjectDescriptorTraits(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        writer: KotlinWriter
    ) = JsonSerdeFieldGenerator.generateSdkObjectDescriptorTraits(ctx, objectShape, writer)

    override val protocol: ShapeId = AwsJson1_0Trait.ID
}

/**
 * Configure the AwsJsonProtocol middleware
 * @param protocolVersion The AWS JSON protocol version (e.g. "1.0", "1.1", etc)
 */
class AwsJsonProtocolMiddleware(private val protocolVersion: String) : ProtocolMiddleware {
    override val name: String = "AwsJsonProtocol"

    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        val awsJsonProtocolSymbol = buildSymbol {
            name = "AwsJsonProtocol"
            namespace(AwsKotlinDependency.AWS_CLIENT_RT_JSON_PROTOCOLS)
        }

        writer.addImport(awsJsonProtocolSymbol)
    }

    override fun renderConfigure(writer: KotlinWriter) {
        writer.write("version = #S", protocolVersion)
    }
}

/**
 * Provides common functionality for SDK serde field generation for JSON-based AWS protocols.
 *
 * TODO ~ move as part of https://github.com/awslabs/smithy-kotlin/issues/260
 */
object JsonSerdeFieldGenerator {

    fun generateSdkFieldDescriptor(
        ctx: ProtocolGenerator.GenerationContext,
        memberShape: MemberShape,
        writer: KotlinWriter,
        memberTargetShape: Shape?,
        namePostfix: String
    ) {
        val serialName = memberShape.getTrait<JsonNameTrait>()?.value ?: memberShape.memberName
        val serialNameTrait = """JsonSerialName("$serialName$namePostfix")"""
        val shapeForSerialKind = memberTargetShape ?: ctx.model.expectShape(memberShape.target)
        val serialKind = shapeForSerialKind.serialKind()
        val descriptorName = memberShape.descriptorName(namePostfix)

        writer.write("private val #L = SdkFieldDescriptor(#L, #L)", descriptorName, serialKind, serialNameTrait)
    }

    fun generateSdkObjectDescriptorTraits(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        writer: KotlinWriter
    ) {
        writer.addImport(KotlinDependency.CLIENT_RT_SERDE.namespace, "*")
        writer.addImport(KotlinDependency.CLIENT_RT_SERDE_JSON.namespace, "JsonSerialName")
        writer.dependencies.addAll(KotlinDependency.CLIENT_RT_SERDE.dependencies)
        writer.dependencies.addAll(KotlinDependency.CLIENT_RT_SERDE_JSON.dependencies)
    }
}
