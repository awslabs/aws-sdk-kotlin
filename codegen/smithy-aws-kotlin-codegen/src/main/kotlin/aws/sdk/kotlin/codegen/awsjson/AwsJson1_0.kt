/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.awsjson

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.buildSymbol
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
            AwsJsonProtocolFeature("1.0"),
            AwsJsonModeledExceptionsFeature(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return parentFeatures + awsJsonFeatures
    }

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        AwsJsonHttpBindingResolver(ctx, "application/x-amz-json-1.0")

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override val protocol: ShapeId = AwsJson1_0Trait.ID
}

/**
 * Configure the AwsJsonProtocol feature
 * @param protocolVersion The AWS JSON protocol version (e.g. "1.0", "1.1", etc)
 */
class AwsJsonProtocolFeature(val protocolVersion: String) : HttpFeature {
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
