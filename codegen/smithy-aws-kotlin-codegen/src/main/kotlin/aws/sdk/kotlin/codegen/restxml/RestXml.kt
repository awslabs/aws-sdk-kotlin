/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.restxml

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.*
import software.amazon.smithy.kotlin.codegen.traits.SyntheticClone
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.ShapeType
import software.amazon.smithy.model.traits.*

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestXml : AwsHttpBindingProtocolGenerator() {

    private val typeReferencableTraitIndex: Map<ShapeId, Symbol> = mapOf(
        XmlNameTrait.ID to AwsRuntimeTypes.SerdeXml.XmlSerialName,
        XmlNamespaceTrait.ID to AwsRuntimeTypes.SerdeXml.XmlNamespace,
        XmlFlattenedTrait.ID to AwsRuntimeTypes.SerdeXml.Flattened,
        XmlAttributeTrait.ID to AwsRuntimeTypes.SerdeXml.XmlAttribute
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
        val traits = traitsForMember(ctx.model, memberShape, namePostfix, writer)
        val shapeForSerialKind = memberTargetShape ?: ctx.model.expectShape(memberShape.target)
        val serialKind = shapeForSerialKind.serialKind()
        val descriptorName = memberShape.descriptorName(namePostfix)

        writer.write("private val #L = SdkFieldDescriptor(#L, #L)", descriptorName, serialKind, traits)

        val traitRefs = (memberShape.allTraits.values + (memberTargetShape?.allTraits?.values ?: emptyList<Trait>())).toSet()
        traitRefs
            .filter { trait -> typeReferencableTraitIndex.containsKey(trait.toShapeId()) }
            .forEach { trait ->
                writer.addImport(typeReferencableTraitIndex[trait.toShapeId()] ?: error("Unable to find symbol for $trait"))
            }
    }

    private fun traitsForMember(model: Model, memberShape: MemberShape, namePostfix: String, writer: KotlinWriter): String {
        val traitList = mutableListOf<String>()

        val serialName = memberShape.getTrait<XmlNameTrait>()?.value ?: memberShape.memberName
        traitList.add("""XmlSerialName("$serialName$namePostfix")""")

        if (memberShape.hasTrait<XmlFlattenedTrait>()) traitList.add("""Flattened""")
        if (memberShape.hasTrait<XmlAttributeTrait>()) traitList.add("""XmlAttribute""")

        val targetShape = model.expectShape(memberShape.target)
        when (targetShape.type) {
            ShapeType.LIST, ShapeType.SET -> {
                val listOrSetMember = if (targetShape.type == ShapeType.LIST) targetShape.asListShape().get().member else targetShape.asSetShape().get().member
                if (listOrSetMember.hasTrait<XmlNameTrait>()) {
                    val memberName = listOrSetMember.expectTrait<XmlNameTrait>().value
                    traitList.add("""XmlCollectionName("$memberName")""")
                    writer.addImport(KotlinDependency.CLIENT_RT_SERDE_XML.namespace, "XmlCollectionName")
                }
            }
            ShapeType.MAP -> {
                val mapMember = targetShape.asMapShape().get()

                val customKeyName = mapMember.key.getTrait<XmlNameTrait>()?.value
                val customValueName = mapMember.value.getTrait<XmlNameTrait>()?.value

                val mapTraitExpr = when {
                    customKeyName != null && customKeyName != null -> """XmlMapName(key = "$customKeyName", value = "$customValueName")"""
                    customKeyName != null -> """XmlMapName(key = "$customKeyName")"""
                    customValueName != null -> """XmlMapName(value = "$customValueName")"""
                    else -> null
                }

                mapTraitExpr?.let {
                    traitList.add(it)
                    writer.addImport(KotlinDependency.CLIENT_RT_SERDE_XML.namespace, "XmlMapName")
                }
            }
        }

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

        val serialName = objectShape.getTrait<XmlNameTrait>()?.value
            ?: objectShape.getTrait<SyntheticClone>()?.archetype?.name
            ?: objectShape.defaultName()

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
