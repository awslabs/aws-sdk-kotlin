/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.restjson

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.JsonSerdeFeature
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.kotlin.codegen.hasIdempotentTokenMember
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.HttpTraitResolver
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.TimestampFormatTrait

// The default Http Binding resolver is used for both white-label smithy-kotlin tests
// and as the restJson1 binding resolver.  If/when AWS-specific logic needs to
// be added to the resolver which is not "white label" in character, these types
// should be broken into two: one purely scoped for white-label SDK testing and one
// for restJson1 support.
typealias RestJsonHttpBindingResolver = HttpTraitResolver

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestJson1 : AwsHttpBindingProtocolGenerator() {

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx)

        val restJsonFeatures = listOf(
            JsonSerdeFeature(ctx.service.hasIdempotentTokenMember(ctx.model)),
            RestJsonErrorFeature(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return features + restJsonFeatures
    }

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override fun getProtocolHttpBindingResolver(generationContext: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        RestJsonHttpBindingResolver(generationContext, "application/json")

    override val protocol: ShapeId = RestJson1Trait.ID
}
