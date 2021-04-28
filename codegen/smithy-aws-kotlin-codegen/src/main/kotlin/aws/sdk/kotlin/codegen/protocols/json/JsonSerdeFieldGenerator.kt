/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.json

import software.amazon.smithy.kotlin.codegen.core.KotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.descriptorName
import software.amazon.smithy.kotlin.codegen.rendering.serde.serialKind
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.JsonNameTrait

/**
 * Provides common functionality for SDK serde field generation for JSON-based AWS protocols.
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
