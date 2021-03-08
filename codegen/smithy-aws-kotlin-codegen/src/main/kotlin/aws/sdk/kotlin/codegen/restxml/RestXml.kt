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
import software.amazon.smithy.model.traits.TimestampFormatTrait
import software.amazon.smithy.model.traits.XmlNameTrait

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestXml : AwsHttpBindingProtocolGenerator() {

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx)

//        val restXmlFeatures = listOf(
//            // TODO - RestXmlError
//        )
//
//        return features + restXmlFeatures
        return features
    }

    override val serdeHandler: SerdeMessageFormatHandler
        get() = XmlMessageFormatHandler()

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    // See https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#content-type
    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/xml")

    override val protocol: ShapeId = RestXmlTrait.ID
}

/**
 * Provides object and field serde codegen for the XML message format.
 */
class XmlMessageFormatHandler : SerdeMessageFormatHandler {
    override fun addSerdeImports(writer: KotlinWriter) {
        writer.addImport(KotlinDependency.CLIENT_RT_SERDE.namespace, "*")
        writer.addImport(KotlinDependency.CLIENT_RT_SERDE_XML.namespace, "XmlSerialName")
        writer.dependencies.addAll(KotlinDependency.CLIENT_RT_SERDE.dependencies)
        writer.dependencies.addAll(KotlinDependency.CLIENT_RT_SERDE_XML.dependencies)
    }

    override fun serialNameTraitForMember(memberShape: MemberShape, namePostfix: String): String {
        val serialName = memberShape.getTrait<XmlNameTrait>()?.value ?: memberShape.memberName
        return """XmlSerialName("$serialName$namePostfix")"""
    }

    override fun serialNameTraitForStruct(objShape: Shape): String? {
        val serialName = objShape.getTrait<XmlNameTrait>()?.value ?: objShape.defaultName()
        return """trait(XmlSerialName("$serialName"))"""
    }
}
