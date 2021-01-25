/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.restxml

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.*
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestXml : AwsHttpBindingProtocolGenerator() {

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx)

        val restXmlFeatures = listOf(
            XmlSerdeFeature(ctx.service.hasIdempotentTokenMember(ctx.model)),
            // TODO - RestXmlError
        )

        return features + restXmlFeatures
    }

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/xml")

    override val protocol: ShapeId = RestXmlTrait.ID
}

class XmlSerdeFeature(generateIdempotencyTokenConfig: Boolean) : HttpSerde("XmlSerdeProvider", generateIdempotencyTokenConfig) {
    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        val xmlSerdeSymbol = buildSymbol {
            name = "XmlSerdeProvider"
            namespace(KotlinDependency.CLIENT_RT_SERDE_XML)
        }
        writer.addImport(xmlSerdeSymbol)
    }
}
