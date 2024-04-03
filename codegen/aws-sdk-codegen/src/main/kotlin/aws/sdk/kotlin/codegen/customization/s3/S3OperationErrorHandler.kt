/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.aws.protocols.core.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.ExceptionBaseClassGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Customized error handling for S3
 */
class S3OperationErrorHandler : KotlinIntegration {

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val service = model.expectShape<ServiceShape>(settings.service)
        return service.isS3
    }

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(AwsHttpBindingProtocolGenerator.Sections.RenderThrowOperationError, overrideThrowOperationErrors))

    private val overrideThrowOperationErrors = SectionWriter { writer, _ ->
        val ctx = writer.getContextValue(AwsHttpBindingProtocolGenerator.Sections.RenderThrowOperationError.Context)
        val op = writer.getContextValue(AwsHttpBindingProtocolGenerator.Sections.RenderThrowOperationError.Operation)
        renderThrowOperationError(ctx, op, writer)
    }

    private fun renderThrowOperationError(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter,
    ) {
        val exceptionBaseSymbol = ExceptionBaseClassGenerator.baseExceptionSymbol(ctx.settings)

        val setS3ErrorMetadata = buildSymbol {
            name = "setS3ErrorMetadata"
            namespace = "${ctx.settings.pkg.name}.internal"
        }

        val parseS3ErrorResponse = buildSymbol {
            name = "parseS3ErrorResponse"
            namespace = "${ctx.settings.pkg.name}.internal"
        }

        val s3ErrorDetails = buildSymbol {
            name = "S3ErrorDetails"
            namespace = "${ctx.settings.pkg.name}.internal"
        }

        writer.write("val wrappedResponse = call.response.#T(payload)", RuntimeTypes.AwsProtocolCore.withPayload)
            .write("val wrappedCall = call.copy(response = wrappedResponse)")
            .write("")
            .write("val errorDetails = try {")
            .indent()
            .call {
                // customize error matching to handle HeadObject/HeadBucket error responses which have no payload
                writer.write("if (payload == null && call.response.status == #T.NotFound) {", RuntimeTypes.Http.StatusCode)
                    .indent()
                    .write("#T(code = #S)", s3ErrorDetails, "NotFound")
                    .dedent()
                    .write("} else {")
                    .indent()
                    .write("""checkNotNull(payload){ "unable to parse error from empty response" }""")
                    .write("#T(payload)", parseS3ErrorResponse)
                    .dedent()
                    .write("}")
            }
            .dedent()
            .withBlock("} catch (ex: Exception) {", "}") {
                withBlock("""throw #T("Failed to parse response as '${ctx.protocol.name}' error", ex).also {""", "}", exceptionBaseSymbol) {
                    write("#T(it, wrappedCall.response, null)", setS3ErrorMetadata)
                }
            }
            .write("")

        writer.withBlock("val ex = when(errorDetails.code) {", "}") {
            op.errors.forEach { err ->
                val errSymbol = ctx.symbolProvider.toSymbol(ctx.model.expectShape(err))
                val errDeserializerSymbol = buildSymbol {
                    name = "${errSymbol.name}Deserializer"
                    namespace = ctx.settings.pkg.serde
                }
                writer.write("#S -> #T().deserialize(context, wrappedCall, payload)", err.name, errDeserializerSymbol)
            }
            write("else -> #T(errorDetails.message)", exceptionBaseSymbol)
        }

        writer.write("")
        writer.write("#T(ex, wrappedResponse, errorDetails)", setS3ErrorMetadata)
        writer.write("throw ex")
    }
}
