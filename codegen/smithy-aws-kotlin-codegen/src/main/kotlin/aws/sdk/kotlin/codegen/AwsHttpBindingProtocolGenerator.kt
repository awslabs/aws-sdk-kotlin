/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.protocoltest.AwsHttpProtocolUnitTestErrorGenerator
import aws.sdk.kotlin.codegen.protocoltest.AwsHttpProtocolUnitTestRequestGenerator
import aws.sdk.kotlin.codegen.protocoltest.AwsHttpProtocolUnitTestResponseGenerator
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinDependency
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.addImport
import software.amazon.smithy.kotlin.codegen.integration.*

/**
 * Base class for all AWS HTTP protocol generators
 */
abstract class AwsHttpBindingProtocolGenerator : HttpBindingProtocolGenerator() {

    override val exceptionBaseClassSymbol: Symbol = Symbol.builder()
        .name("AwsServiceException")
        .namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE.namespace, ".")
        .addDependency(AwsKotlinDependency.AWS_CLIENT_RT_CORE)
        .build()

    override fun getHttpProtocolClientGenerator(ctx: ProtocolGenerator.GenerationContext): HttpProtocolClientGenerator {
        val rootNamespace = ctx.settings.moduleName
        val features = getHttpFeatures(ctx)
        return AwsHttpProtocolClientGenerator(ctx, rootNamespace, features, getProtocolHttpBindingResolver(ctx))
    }

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx).toMutableList()

        features.add(EndpointResolverFeature(ctx))
        if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.service)) {
            val signingName = AwsSignatureVersion4.signingServiceName(ctx.model, ctx.service)
            features.add(AwsSignatureVersion4(signingName))
        }

        return features
    }

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {
        val ignoredTests = TestMemberDelta(
            setOf(
                // FIXME - document type not fully supported yet
                // restJson
                "InlineDocumentInput",
                "InlineDocumentAsPayloadInput",
                "InlineDocumentOutput",
                "InlineDocumentAsPayloadInputOutput",

                // awsJson1.1
                "PutAndGetInlineDocumentsInput"
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
