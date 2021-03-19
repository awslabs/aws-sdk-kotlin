/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.restxml

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.*
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.*

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestXml : AwsHttpBindingProtocolGenerator() {

    private val typeReferencableTraitIndex: Map<ShapeId, String> = mapOf(
        XmlNameTrait.ID to "XmlSerialName",
        XmlNamespaceTrait.ID to "XmlNamespace",
        XmlFlattenedTrait.ID to "Flattened",
        XmlAttributeTrait.ID to "XmlAttribute"
    )

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx)

//        val restXmlFeatures = listOf(
//            // TODO - RestXmlError
//        )
//
//        return features + restXmlFeatures
        return features
    }

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    // See https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#content-type
    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/xml")

    override fun generateSdkFieldDescriptor(
        ctx: ProtocolGenerator.GenerationContext,
        memberShape: MemberShape,
        writer: KotlinWriter,
        memberTargetShape: Shape?,
        namePostfix: String
    ) {
        val traits = traitsForMember(memberShape, namePostfix)
        val shapeForSerialKind = memberTargetShape ?: ctx.model.expectShape(memberShape.target)
        val serialKind = shapeForSerialKind.serialKind()
        val descriptorName = memberShape.descriptorName(namePostfix)

        writer.write("private val #L = SdkFieldDescriptor(#L, #L)", descriptorName, serialKind, traits)

        val traitRefs = (memberShape.allTraits.values + (memberTargetShape?.allTraits?.values ?: emptyList<Trait>())).toSet()
        traitRefs
            .filter { trait -> typeReferencableTraitIndex.containsKey(trait.toShapeId()) }
            .forEach { trait ->
                writer.addImport(KotlinDependency.CLIENT_RT_SERDE_XML.namespace, typeReferencableTraitIndex[trait.toShapeId()]!!)
            }
    }

    private fun traitsForMember(memberShape: MemberShape, namePostfix: String): String {
        val traitList = mutableListOf<String>()

        val serialName = memberShape.getTrait<XmlNameTrait>()?.value ?: memberShape.memberName
        traitList.add("""XmlSerialName("$serialName$namePostfix")""")

        if (memberShape.hasTrait<XmlFlattenedTrait>()) traitList.add("""Flattened""")
        if (memberShape.hasTrait<XmlAttributeTrait>()) traitList.add("""XmlAttribute("$serialName$namePostfix")""")

        return traitList.joinToString(separator = ", ")
    }

    override fun generateSdkObjectDescriptorTraits(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        writer: KotlinWriter
    ) {
        writer.addImport(KotlinDependency.CLIENT_RT_SERDE.namespace, "*")
        writer.addImport(KotlinDependency.CLIENT_RT_SERDE_XML.namespace, "XmlSerialName")
        writer.dependencies.addAll(KotlinDependency.CLIENT_RT_SERDE.dependencies)
        writer.dependencies.addAll(KotlinDependency.CLIENT_RT_SERDE_XML.dependencies)

        val serialName = objectShape.getTrait<XmlNameTrait>()?.value ?: objectShape.defaultName()
        writer.write("""trait(XmlSerialName("$serialName"))""")

        if (objectShape.hasTrait<XmlNamespaceTrait>()) {
            writer.addImport(KotlinDependency.CLIENT_RT_SERDE_XML.namespace, "XmlNamespace")
            val namespaceTrait = objectShape.expectTrait<XmlNamespaceTrait>()

            when (val prefix = namespaceTrait.prefix.getOrNull()) {
                null -> writer.write("""trait(XmlNamespace("${namespaceTrait.uri}"))""")
                else -> writer.write("""trait(XmlNamespace("${namespaceTrait.uri}", "$prefix"))""")
            }
        }
    }

    override val protocol: ShapeId = RestXmlTrait.ID
}
