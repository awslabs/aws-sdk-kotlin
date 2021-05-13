/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.protocols.middleware.AwsSignatureVersion4
import aws.sdk.kotlin.codegen.protocols.middleware.EndpointResolverMiddleware
import aws.sdk.kotlin.codegen.protocols.middleware.UserAgentMiddleware
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestErrorGenerator
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestRequestGenerator
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestResponseGenerator
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*

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
        val middleware = getHttpMiddleware(ctx)
        return AwsHttpProtocolClientGenerator(ctx, middleware, getProtocolHttpBindingResolver(ctx))
    }

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx).toMutableList()

        middleware.add(EndpointResolverMiddleware(ctx))
        if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.service)) {
            val signingName = AwsSignatureVersion4.signingServiceName(ctx.model, ctx.service)
            middleware.add(AwsSignatureVersion4(signingName))
        }

        middleware.add(UserAgentMiddleware())
        return middleware
    }

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {
        val ignoredTests = TestMemberDelta(
            setOf(
                // FIXME - document type not fully supported yet
                // restJson
                "InlineDocumentInput",
                "InlineDocumentAsPayloadInput",
                "InlineDocumentOutput",
                "InlineDocumentAsPayloadInputOutput", // See https://github.com/awslabs/smithy-kotlin/issues/123
                // new in Smithy 1.7.0
                "RestJsonQueryPrecedence",
                "RestJsonQueryParamsStringListMap",
                "RestJsonAllQueryStringTypes", // See https://github.com/awslabs/smithy-kotlin/issues/285

                // awsJson1.1
                "PutAndGetInlineDocumentsInput",

                // restXml
                "IgnoreQueryParamsInResponse", // See https://github.com/awslabs/smithy/issues/756, Remove after upgrading past Smithy 1.7.0
                // new in Smithy 1.7.0
                "RestXmlQueryPrecedence", // See https://github.com/awslabs/smithy-kotlin/issues/285
                "RestXmlQueryParamsStringListMap",
                "AllQueryStringTypes"
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
