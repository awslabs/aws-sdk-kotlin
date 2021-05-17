/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.xml.RestXmlErrorMiddleware
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.model.traits.SyntheticClone
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpTraitResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.serde.descriptorName
import software.amazon.smithy.kotlin.codegen.rendering.serde.serialKind
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestXml : AwsHttpBindingProtocolGenerator() {

    private val typeReferencableTraitIndex: Map<ShapeId, Symbol> = mapOf(
        XmlNameTrait.ID to RuntimeTypes.Serde.SerdeXml.XmlSerialName,
        XmlNamespaceTrait.ID to RuntimeTypes.Serde.SerdeXml.XmlNamespace,
        XmlFlattenedTrait.ID to RuntimeTypes.Serde.SerdeXml.Flattened,
        XmlAttributeTrait.ID to RuntimeTypes.Serde.SerdeXml.XmlAttribute
    )

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx)

        val restXmlMiddleware = listOf(
            RestXmlErrorMiddleware(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return middleware + restXmlMiddleware
    }

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    // See https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#content-type
    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/xml")

    override fun sortMembersForSerialization(
        ctx: ProtocolGenerator.GenerationContext,
        members: List<MemberShape>
    ): List<MemberShape> {
        val attributes = members.filter { it.hasTrait<XmlAttributeTrait>() }.sortedBy { it.memberName }
        val elements = members.filterNot { it.hasTrait<XmlAttributeTrait>() }.sortedBy { it.memberName }

        // XML attributes MUST be serialized immediately following calls to `startTag` before
        // any nested content is serialized
        return attributes + elements
    }

    override fun generateSdkFieldDescriptor(
        ctx: ProtocolGenerator.GenerationContext,
        memberShape: MemberShape,
        writer: KotlinWriter,
        memberTargetShape: Shape?,
        namePostfix: String
    ) {
        val traits = traitsForMember(ctx.model, memberShape, memberTargetShape, namePostfix, writer)
        val shapeForSerialKind = memberTargetShape ?: ctx.model.expectShape(memberShape.target)
        val serialKind = shapeForSerialKind.serialKind()
        val descriptorName = memberShape.descriptorName(namePostfix)

        if (traits.isEmpty()) {
            writer.write("private val #L = SdkFieldDescriptor(#L)", descriptorName, serialKind)
        } else {
            writer.write("private val #L = SdkFieldDescriptor(#L, #L)", descriptorName, serialKind, traits)
        }

        val traitRefs = (memberShape.allTraits.values + (memberTargetShape?.allTraits?.values ?: emptyList<Trait>())).toSet()
        traitRefs
            .filter { trait -> typeReferencableTraitIndex.containsKey(trait.toShapeId()) }
            .forEach { trait ->
                writer.addImport(typeReferencableTraitIndex[trait.toShapeId()] ?: error("Unable to find symbol for $trait"))
            }
    }

    private fun traitsForMember(
        model: Model,
        memberShape: MemberShape,
        memberTargetShape: Shape?,
        namePostfix: String,
        writer: KotlinWriter
    ): String {
        val traitList = mutableListOf<String>()

        val serialName = when (namePostfix.isEmpty()) {
            true -> memberShape.getTrait<XmlNameTrait>()?.value ?: memberShape.memberName
            false -> memberShape.getTrait<XmlNameTrait>()?.value ?: "member"
        }
        traitList.add("""XmlSerialName("$serialName")""")

        val memberTarget = model.expectShape(memberShape.target)
        val isNestedMap = memberTarget.isMapShape && (memberTargetShape?.isMapShape ?: false)
        val flattened = memberShape.getTrait<XmlFlattenedTrait>()?.also {
            if (!isNestedMap) {
                traitList.add(it.toSerdeFieldTraitSpec())
            }
        } != null

        memberShape.getTrait<XmlAttributeTrait>()?.let { traitList.add(it.toSerdeFieldTraitSpec()) }
        memberShape.getTrait<XmlNamespaceTrait>()?.let { traitList.add(it.toSerdeFieldTraitSpec()) }

        val targetShape = model.expectShape(memberShape.target)
        when (targetShape.type) {
            ShapeType.LIST, ShapeType.SET -> {
                val collectionMember = (targetShape as CollectionShape).member
                if (!flattened && collectionMember.hasTrait<XmlNameTrait>()) {
                    // flattened collections should only need the XmlSerialName trait since there is no <member> element
                    val memberName = collectionMember.expectTrait<XmlNameTrait>().value
                    traitList.add("""XmlCollectionName("$memberName")""")
                    writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlCollectionName)
                }

                if (collectionMember.hasTrait<XmlNamespaceTrait>()) {
                    val ns = collectionMember.expectTrait<XmlNamespaceTrait>()
                    val nsTrait = ns.toSerdeFieldTraitSpec("XmlCollectionValueNamespace")
                    traitList.add(nsTrait)
                    writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlCollectionValueNamespace)
                }
            }
            ShapeType.MAP -> {
                val mapMember = targetShape as MapShape

                val customKeyName = mapMember.key.getTrait<XmlNameTrait>()?.value
                val customValueName = mapMember.value.getTrait<XmlNameTrait>()?.value

                val mapTraitExpr = when {
                    customKeyName != null && customValueName != null -> """XmlMapName(key = "$customKeyName", value = "$customValueName")"""
                    customKeyName != null -> """XmlMapName(key = "$customKeyName")"""
                    customValueName != null -> """XmlMapName(value = "$customValueName")"""
                    else -> null
                }

                mapTraitExpr?.let {
                    traitList.add(it)
                    writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlMapName)
                }

                mapMember.key
                    .getTrait<XmlNamespaceTrait>()
                    ?.toSerdeFieldTraitSpec("XmlMapKeyNamespace")
                    ?.let {
                        traitList.add(it)
                        writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlMapKeyNamespace)
                    }

                mapMember.value
                    .getTrait<XmlNamespaceTrait>()
                    ?.toSerdeFieldTraitSpec("XmlCollectionValueNamespace")
                    ?.let {
                        traitList.add(it)
                        writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlCollectionValueNamespace)
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

        val serialName = when {
            objectShape.hasTrait<HttpErrorTrait>() -> "Error"
            objectShape.hasTrait<XmlNameTrait>() -> objectShape.expectTrait<XmlNameTrait>().value
            objectShape.hasTrait<SyntheticClone>() -> objectShape.expectTrait<SyntheticClone>().archetype.name
            else -> objectShape.defaultName(ctx.service)
        }

        writer.write("""trait(XmlSerialName("$serialName"))""")

        if (objectShape.hasTrait<HttpErrorTrait>()) {
            writer.addImport(KotlinDependency.CLIENT_RT_SERDE_XML.namespace, "XmlError")
            writer.write("""trait(XmlError)""")
        }

        // namespace trait if present comes from the struct or falls back to the service
        val namespaceTrait: XmlNamespaceTrait? = objectShape.getTrait() ?: ctx.service.getTrait()
        if (namespaceTrait != null) {
            writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlNamespace)
            val serdeTrait = namespaceTrait.toSerdeFieldTraitSpec()
            writer.write("""trait($serdeTrait)""")
        }
    }

    override val protocol: ShapeId = RestXmlTrait.ID
}

private fun XmlNamespaceTrait.toSerdeFieldTraitSpec(namespaceTrait: String = "XmlNamespace"): String =
    if (prefix.isPresent) {
        """$namespaceTrait("${this.uri}", "${this.prefix.get()}")"""
    } else {
        """$namespaceTrait("${this.uri}")"""
    }

private fun XmlAttributeTrait.toSerdeFieldTraitSpec(): String = "XmlAttribute"

private fun XmlFlattenedTrait.toSerdeFieldTraitSpec(): String = "Flattened"
