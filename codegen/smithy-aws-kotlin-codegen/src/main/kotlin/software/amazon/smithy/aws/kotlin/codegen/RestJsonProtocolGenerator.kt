/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinDependency
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.hasIdempotentTokenMember
import software.amazon.smithy.kotlin.codegen.integration.*
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Shared base protocol generator for all AWS JSON protocol variants
 */
abstract class RestJsonProtocolGenerator : AwsHttpBindingProtocolGenerator() {
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {
        val ignoredTests = TestMemberDelta(
            setOf(
                // FIXME - document type not fully supported yet
                "InlineDocumentInput",
                "InlineDocumentAsPayloadInput",
                "InlineDocumentOutput",
                "InlineDocumentAsPayloadInputOutput"
            ),
            TestContainmentMode.EXCLUDE_TESTS
        )

        // The following can be used to generate only a specific test by name.
        // val targetedTest = TestMemberDelta(setOf("RestJsonComplexErrorWithNoMessage"), TestContainmentMode.RUN_TESTS)

        val requestTestBuilder = AwsHttpProtocolUnitTestRequestGenerator.Builder()
        val responseTestBuilder = AwsHttpProtocolUnitTestResponseGenerator.Builder()
        val errorTestBuilder = AwsHttpProtocolUnitTestErrorGenerator.Builder()

        HttpProtocolTestGenerator(
            ctx,
            requestTestBuilder,
            responseTestBuilder,
            errorTestBuilder,
            ignoredTests
        ).generateProtocolTests()
    }

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx).toMutableList()
        val jsonFeatures = listOf(
            JsonSerdeFeature(ctx.service.hasIdempotentTokenMember(ctx.model)),
            RestJsonErrorFeature(ctx)
        )
        features.addAll(jsonFeatures)
        return features
    }
}

class JsonSerdeFeature(generateIdempotencyTokenConfig: Boolean) : HttpSerde("JsonSerdeProvider", generateIdempotencyTokenConfig) {
    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        val jsonSerdeSymbol = Symbol.builder()
            .name("JsonSerdeProvider")
            .namespace(KotlinDependency.CLIENT_RT_SERDE_JSON.namespace, ".")
            .addDependency(KotlinDependency.CLIENT_RT_SERDE_JSON)
            .build()
        writer.addImport(jsonSerdeSymbol, "")
    }
}

class RestJsonErrorFeature(ctx: ProtocolGenerator.GenerationContext) : ModeledExceptionsFeature(ctx) {
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
}
