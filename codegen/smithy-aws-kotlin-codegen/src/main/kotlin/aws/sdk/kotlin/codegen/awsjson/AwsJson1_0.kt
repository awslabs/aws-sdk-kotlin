/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.awsjson

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.JsonSerdeFeature
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.buildSymbol
import software.amazon.smithy.kotlin.codegen.hasIdempotentTokenMember
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.namespace
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Handles generating the aws.protocols#awsJson1_0 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class AwsJson1_0 : AwsHttpBindingProtocolGenerator() {

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val parentFeatures = super.getHttpFeatures(ctx)
        val awsJsonFeatures = listOf(
            JsonSerdeFeature(ctx.service.hasIdempotentTokenMember(ctx.model)),
            AwsJsonTargetHeaderFeature(),
            AwsJsonModeledExceptionsFeature(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return parentFeatures + awsJsonFeatures
    }

    override fun getProtocolHttpBindingResolver(generationContext: ProtocolGenerator.GenerationContext): HttpBindingResolver = AwsJsonHttpBindingResolver(generationContext)

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override val protocol: ShapeId = AwsJson1_0Trait.ID
}

class AwsJsonTargetHeaderFeature : HttpFeature {
    override val name: String = "AwsJsonTargetHeader"

    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        val awsJsonTargetHeaderSymbol = buildSymbol {
            name = "AwsJsonTargetHeader"
            namespace(AwsKotlinDependency.REST_JSON_FEAT)
            dependency(AwsKotlinDependency.REST_JSON_FEAT)
        }

        writer.addImport(awsJsonTargetHeaderSymbol, "")
    }
}
