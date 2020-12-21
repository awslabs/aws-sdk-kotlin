/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.DefaultHttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.HttpErrorTrait

// The default Http Binding resolver is used for both white-label smithy-kotlin tests
// and as the restJson1 binding resolver.  If/when AWS-specific logic needs to
// be added to the resolver which is not "white label" in character, these types
// should be broken into two: one purely scoped for white-label SDK testing and one
// for restJson1 support.
typealias RestJsonHttpBindingResolver = DefaultHttpBindingResolver

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see RestJsonProtocolGenerator
 */
class RestJson1 : AwsHttpBindingProtocolGenerator() {
    class RestJsonErrorFeature(ctx: ProtocolGenerator.GenerationContext,
                               httpBindingResolver: HttpBindingResolver
    ) : ModeledExceptionsFeature(ctx, httpBindingResolver) {
        override val name: String = "RestJsonError"

        override fun addImportsAndDependencies(writer: KotlinWriter) {
            super.addImportsAndDependencies(writer)
            val restJsonSymbol = Symbol.builder()
                .name("RestJsonError")
                .namespace(AwsKotlinDependency.REST_JSON_FEAT.namespace, ".")
                .addDependency(AwsKotlinDependency.REST_JSON_FEAT)
                .build()
            writer.addImport(restJsonSymbol, "")
        }

        override fun renderConfigure(writer: KotlinWriter) {
            val errors = getModeledErrors()

            errors.forEach { errShape ->
                val code = errShape.id.name
                val symbol = ctx.symbolProvider.toSymbol(errShape)
                val deserializerName = "${symbol.name}Deserializer"
                val httpStatusCode: Int? = errShape.getTrait(HttpErrorTrait::class.java).map { it.code }.orElse(null)
                if (httpStatusCode != null) {
                    writer.write("register(code = \$S, deserializer = $deserializerName(), httpStatusCode = $httpStatusCode)", code)
                }
            }
        }
    }

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx)

        return features + listOf(RestJsonErrorFeature(ctx, getProtocolHttpBindingResolver(ctx)))
    }

    override fun getProtocolHttpBindingResolver(generationContext: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        RestJsonHttpBindingResolver(generationContext)

    override val protocol: ShapeId = RestJson1Trait.ID
}
