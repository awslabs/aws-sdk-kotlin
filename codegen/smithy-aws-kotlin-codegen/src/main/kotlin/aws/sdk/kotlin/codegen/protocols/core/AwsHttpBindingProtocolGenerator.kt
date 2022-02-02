/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.middleware.AwsSignatureVersion4
import aws.sdk.kotlin.codegen.protocols.middleware.ResolveAwsEndpointMiddleware
import aws.sdk.kotlin.codegen.protocols.middleware.UserAgentMiddleware
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestErrorGenerator
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestRequestGenerator
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestResponseGenerator
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.ExceptionBaseClassGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ShapeId

/**
 * Base class for all AWS HTTP protocol generators
 */
abstract class AwsHttpBindingProtocolGenerator : HttpBindingProtocolGenerator() {

    override val exceptionBaseClassSymbol: Symbol = buildSymbol {
        name = "AwsServiceException"
        namespace(AwsKotlinDependency.AWS_CORE)
    }

    override fun getHttpProtocolClientGenerator(ctx: ProtocolGenerator.GenerationContext): HttpProtocolClientGenerator {
        val middleware = getHttpMiddleware(ctx)
        return AwsHttpProtocolClientGenerator(ctx, middleware, getProtocolHttpBindingResolver(ctx.model, ctx.service))
    }

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx)
            // endpoint resolver is customized for AWS services - need to replace the default one from smithy-kotlin
            .replace(ResolveAwsEndpointMiddleware(ctx)) {
                it is ResolveEndpointMiddleware
            }.toMutableList()

        if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.service)) {
            val signingName = AwsSignatureVersion4.signingServiceName(ctx.service)
            middleware.add(AwsSignatureVersion4(signingName))
        }

        middleware.add(UserAgentMiddleware())
        return middleware
    }

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {
        val ignoredTests = TestMemberDelta(
            setOf(
                // restJson
                // FIXME - document type not fully supported yet, see https://github.com/awslabs/smithy-kotlin/issues/123
                "DocumentTypeInputWithObject",
                "DocumentTypeAsPayloadInput",
                "DocumentTypeAsPayloadInputString",
                "DocumentOutput",
                "DocumentTypeAsPayloadOutput",
                "DocumentTypeAsPayloadOutputString",
                "DocumentInputWithString",
                "DocumentInputWithNumber",
                "DocumentInputWithBoolean",
                "DocumentInputWithList",
                "DocumentOutputString",
                "DocumentOutputNumber",
                "DocumentOutputBoolean",
                "DocumentOutputArray",

                // awsJson1.1
                // FIXME - document type not fully supported yet, see https://github.com/awslabs/smithy-kotlin/issues/123
                "PutAndGetInlineDocumentsInput",

                // smithy-kotlin#519
                "SimpleScalarPropertiesWithWhiteSpace",
                "SimpleScalarPropertiesPureWhiteSpace",
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

    /**
     * Get the error "code" that uniquely identifies the AWS error.
     */
    protected open fun getErrorCode(ctx: ProtocolGenerator.GenerationContext, errShapeId: ShapeId): String = errShapeId.name

    /**
     * Render the code to parse the `ErrorDetails` from the HTTP response.
     */
    abstract fun renderDeserializeErrorDetails(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter)

    override fun operationErrorHandler(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Symbol =
        op.errorHandler(ctx.settings) { writer ->
            writer.withBlock(
                "private suspend fun ${op.errorHandlerName()}(context: #T, response: #T): #Q {",
                "}",
                RuntimeTypes.Core.ExecutionContext,
                RuntimeTypes.Http.Response.HttpResponse,
                KotlinTypes.Nothing
            ) {
                renderThrowOperationError(ctx, op, writer)
            }
        }

    protected open fun renderThrowOperationError(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter
    ) {
        val exceptionBaseSymbol = ExceptionBaseClassGenerator.baseExceptionSymbol(ctx.settings)
        writer.write("val payload = response.body.#T()", RuntimeTypes.Http.readAll)
            .write("val wrappedResponse = response.#T(payload)", AwsRuntimeTypes.Http.withPayload)
            .write("")
            .write("val errorDetails = try {")
            .indent()
            .call {
                renderDeserializeErrorDetails(ctx, op, writer)
            }
            .dedent()
            .withBlock("} catch (ex: Exception) {", "}") {
                withBlock("""throw #T("Failed to parse response as '${ctx.protocol.name}' error", ex).also {""", "}", exceptionBaseSymbol) {
                    write("#T(it, wrappedResponse, null)", AwsRuntimeTypes.Http.setAseErrorMetadata)
                }
            }
            .write("")

        writer.withBlock("val ex = when(errorDetails.code) {", "}") {
            op.errors.forEach { err ->
                val errSymbol = ctx.symbolProvider.toSymbol(ctx.model.expectShape(err))
                val errDeserializerSymbol = buildSymbol {
                    name = "${errSymbol.name}Deserializer"
                    namespace = "${ctx.settings.pkg.name}.transform"
                }
                writer.write("#S -> #T().deserialize(context, wrappedResponse)", getErrorCode(ctx, err), errDeserializerSymbol)
            }
            write("else -> #T(errorDetails.message)", exceptionBaseSymbol)
        }

        writer.write("")
        writer.write("#T(ex, wrappedResponse, errorDetails)", AwsRuntimeTypes.Http.setAseErrorMetadata)
        writer.write("throw ex")
    }
}
