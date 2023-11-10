/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.AwsEndpointDelegator
import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.protocols.eventstream.EventStreamParserGenerator
import aws.sdk.kotlin.codegen.protocols.eventstream.EventStreamSerializerGenerator
import aws.sdk.kotlin.codegen.protocols.middleware.AwsSpanInterceptorMiddleware
import aws.sdk.kotlin.codegen.protocols.middleware.RecursionDetectionMiddleware
import aws.sdk.kotlin.codegen.protocols.middleware.UserAgentMiddleware
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestErrorGenerator
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestRequestGenerator
import aws.sdk.kotlin.codegen.protocols.protocoltest.AwsHttpProtocolUnitTestResponseGenerator
import software.amazon.smithy.aws.traits.protocols.AwsQueryCompatibleTrait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.SectionId
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.ExceptionBaseClassGenerator
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointDelegator
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
        val middleware = super.getDefaultHttpMiddleware(ctx).toMutableList()
        middleware.add(UserAgentMiddleware())
        middleware.add(RecursionDetectionMiddleware())
        middleware.add(AwsSpanInterceptorMiddleware())
        return middleware
    }

    override fun generateProtocolUnitTests(ctx: ProtocolGenerator.GenerationContext) {
        val ignoredTests = TestMemberDelta(
            setOf(
                // FIXME - compression not yet supported, see https://github.com/awslabs/smithy-kotlin/issues/955
                "SDKAppliedContentEncoding_awsJson1_0",
                "SDKAppliedContentEncoding_awsJson1_1",
                "SDKAppliedContentEncoding_awsQuery",
                "SDKAppliedContentEncoding_ec2Query",
                "SDKAppliedContentEncoding_restJson1",
                "SDKAppliedContentEncoding_restXml",
                "SDKAppendedGzipAfterProvidedEncoding_restJson1",
                "SDKAppendedGzipAfterProvidedEncoding_restXml",
                "SDKAppendsGzipAndIgnoresHttpProvidedEncoding_awsJson1_0",
                "SDKAppendsGzipAndIgnoresHttpProvidedEncoding_awsJson1_1",
                "SDKAppendsGzipAndIgnoresHttpProvidedEncoding_awsQuery",
                "SDKAppendsGzipAndIgnoresHttpProvidedEncoding_ec2Query",
            ),
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
            ignoredTests,
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

    override fun eventStreamRequestHandler(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Symbol {
        val resolver = getProtocolHttpBindingResolver(ctx.model, ctx.service)
        val contentType = resolver.determineRequestContentType(op) ?: error("event streams must set a content-type")
        val eventStreamSerializerGenerator = EventStreamSerializerGenerator(structuredDataSerializer(ctx), contentType)
        return eventStreamSerializerGenerator.requestHandler(ctx, op)
    }

    override fun eventStreamResponseHandler(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Symbol {
        val eventStreamParserGenerator = EventStreamParserGenerator(ctx, structuredDataParser(ctx))
        return eventStreamParserGenerator.responseHandler(ctx, op)
    }

    override fun operationErrorHandler(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Symbol =
        op.errorHandler(ctx.settings) { writer ->
            writer.withBlock(
                "private suspend fun ${op.errorHandlerName()}(context: #T, call: #T): #Q {",
                "}",
                RuntimeTypes.Core.ExecutionContext,
                RuntimeTypes.Http.HttpCall,
                KotlinTypes.Nothing,
            ) {
                renderThrowOperationError(ctx, op, writer)
            }
        }

    object ProtocolErrorDeserialization : SectionId

    protected open fun renderThrowOperationError(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter,
    ) {
        val exceptionBaseSymbol = ExceptionBaseClassGenerator.baseExceptionSymbol(ctx.settings)
        writer.write("val payload = call.response.body.#T()", RuntimeTypes.Http.readAll)
            .write("val wrappedResponse = call.response.#T(payload)", RuntimeTypes.AwsProtocolCore.withPayload)
            .write("val wrappedCall = call.copy(response = wrappedResponse)")
            .write("")
            .declareSection(ProtocolErrorDeserialization)
            .write("val errorDetails = try {")
            .indent()
            .call {
                renderDeserializeErrorDetails(ctx, op, writer)
            }
            .dedent()
            .withBlock("} catch (ex: Exception) {", "}") {
                withBlock("""throw #T("Failed to parse response as '${ctx.protocol.name}' error", ex).also {""", "}", exceptionBaseSymbol) {
                    write("#T(it, wrappedCall.response, null)", RuntimeTypes.AwsProtocolCore.setAseErrorMetadata)
                }
            }
            .write("")

        if (ctx.service.hasTrait<AwsQueryCompatibleTrait>()) {
            writer.write("var queryErrorDetails: #T? = null", RuntimeTypes.AwsProtocolCore.AwsQueryCompatibleErrorDetails)
            writer.withBlock("call.response.headers[#T]?.let {", "}", RuntimeTypes.AwsProtocolCore.XAmznQueryErrorHeader) {
                openBlock("queryErrorDetails = try {")
                write("#T.parse(it)", RuntimeTypes.AwsProtocolCore.AwsQueryCompatibleErrorDetails)
                closeAndOpenBlock("} catch (ex: Exception) {")
                withBlock("""throw #T("Failed to parse awsQuery-compatible error", ex).also {""", "}", exceptionBaseSymbol) {
                    write("#T(it, wrappedResponse, errorDetails)", RuntimeTypes.AwsProtocolCore.setAseErrorMetadata)
                }
                closeBlock("}")
            }
            writer.write("")
        }

        writer.withBlock("val ex = when(errorDetails.code) {", "}") {
            op.errors.forEach { err ->
                val errSymbol = ctx.symbolProvider.toSymbol(ctx.model.expectShape(err))
                val errDeserializerSymbol = buildSymbol {
                    name = "${errSymbol.name}Deserializer"
                    namespace = ctx.settings.pkg.serde
                }
                writer.write("#S -> #T().deserialize(context, wrappedCall)", getErrorCode(ctx, err), errDeserializerSymbol)
            }
            write("else -> #T(errorDetails.message)", exceptionBaseSymbol)
        }

        writer.write("")
        writer.write("#T(ex, wrappedResponse, errorDetails)", RuntimeTypes.AwsProtocolCore.setAseErrorMetadata)
        if (ctx.service.hasTrait<AwsQueryCompatibleTrait>()) {
            writer.write("queryErrorDetails?.let { #T(ex, it) }", RuntimeTypes.AwsProtocolCore.setAwsQueryCompatibleErrorMetadata)
        }

        writer.write("throw ex")
    }

    override fun endpointDelegator(ctx: ProtocolGenerator.GenerationContext): EndpointDelegator = AwsEndpointDelegator()
}
