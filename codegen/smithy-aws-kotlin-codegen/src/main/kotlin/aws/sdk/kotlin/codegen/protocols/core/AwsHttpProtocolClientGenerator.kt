/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.AwsRuntimeTypes.Core.Client.AwsClientOption
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes.SmithyClient.SdkClientOption
import software.amazon.smithy.kotlin.codegen.integration.SectionId
import software.amazon.smithy.kotlin.codegen.integration.SectionKey
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.model.hasIdempotentTokenMember
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.knowledge.ServiceIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.traits.HttpBearerAuthTrait

/**
 * Override for generating concrete (AWS) HTTP service clients
 */
open class AwsHttpProtocolClientGenerator(
    ctx: ProtocolGenerator.GenerationContext,
    middlewares: List<ProtocolMiddleware>,
    httpBindingResolver: HttpBindingResolver,
) : HttpProtocolClientGenerator(ctx, middlewares, httpBindingResolver) {
    object MergeServiceDefaults : SectionId {
        val GenerationContext: SectionKey<ProtocolGenerator.GenerationContext> = SectionKey("GenerationContext")
    }

    override fun render(writer: KotlinWriter) {
        writer.write("\n\n")
        writer.write(
            "#L const val ServiceApiVersion: String = #S",
            ctx.settings.api.visibility,
            ctx.service.version,
        )
        writer.write("\n\n")
        // set AWS specific span attributes for an operation
        // https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/instrumentation/aws-sdk/
        val addAwsSpanAttrWriter = SectionWriter { w, _ ->
            w.withBlock("attributes = #T {", "}", RuntimeTypes.Core.Utils.attributesOf) {
                write("#S to #S", "rpc.system", "aws-api")
            }
        }
        writer.registerSectionWriter(OperationTelemetryBuilder, addAwsSpanAttrWriter)
        super.render(writer)
    }

    override fun renderInit(writer: KotlinWriter) {
        writer.withBlock("init {", "}") {
            write("managedResources.#T(config.httpClient)", RuntimeTypes.Core.IO.addIfManaged)

            if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.settings.getService(ctx.model))) {
                write("managedResources.#T(config.credentialsProvider)", RuntimeTypes.Core.IO.addIfManaged)
            }

            val serviceIndex = ServiceIndex.of(ctx.model)
            val hasBearerTokenAuth = serviceIndex
                .getAuthSchemes(ctx.settings.service)
                .containsKey(HttpBearerAuthTrait.ID)
            if (hasBearerTokenAuth) {
                write("managedResources.#T(config.bearerTokenProvider)", RuntimeTypes.Core.IO.addIfManaged)
            }
        }
    }

    override fun renderOperationSetup(writer: KotlinWriter, opIndex: OperationIndex, op: OperationShape) {
        super.renderOperationSetup(writer, opIndex, op)

        // override the default retry policy
        writer.write("op.execution.retryPolicy = config.retryPolicy")

        writer.write("mergeServiceDefaults(op.context)")
    }

    override fun renderAdditionalMethods(writer: KotlinWriter) {
        renderMergeServiceDefaults(writer)
    }

    /**
     * render a utility function to populate an operation's ExecutionContext with defaults from service config, environment, etc
     */
    private fun renderMergeServiceDefaults(writer: KotlinWriter) {
        // FIXME - we likely need a way to let customizations modify/override this
        // FIXME - we also need a way to tie in config properties added via integrations that need to influence the context
        writer.dokka("merge the defaults configured for the service into the execution context before firing off a request")
        writer.withBlock(
            "private fun mergeServiceDefaults(ctx: #T) {",
            "}",
            RuntimeTypes.Core.ExecutionContext,
        ) {
            putIfAbsent(AwsClientOption, "Region", nullable = true)
            putIfAbsent(SdkClientOption, "ClientName")
            putIfAbsent(SdkClientOption, "LogMode")
            // fill in auth/signing attributes
            if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.service)) {
                // default signing context (most of this has been moved to auth schemes but some things like event streams still depend on this)
                val signingServiceName = AwsSignatureVersion4.signingServiceName(ctx.service)
                putIfAbsent(AwsSigningAttributes, "SigningService", signingServiceName.dq())
                putIfAbsent(AwsSigningAttributes, "SigningRegion", "config.region", nullable = true)
                putIfAbsent(AwsSigningAttributes, "CredentialsProvider")
            }

            if (ctx.service.hasIdempotentTokenMember(ctx.model)) {
                putIfAbsent(SdkClientOption, "IdempotencyTokenProvider", nullable = true)
            }

            writer.declareSection(MergeServiceDefaults)
        }
    }
}

internal fun KotlinWriter.putIfAbsent(
    attributesSymbol: Symbol,
    name: String,
    literalValue: String? = null,
    nullable: Boolean = false,
) {
    val putSymbol = if (nullable) RuntimeTypes.Core.Utils.putIfAbsentNotNull else RuntimeTypes.Core.Utils.putIfAbsent
    val actualValue = literalValue ?: "config.${name.replaceFirstChar(Char::lowercaseChar)}"
    write("ctx.#T(#T.#L, #L)", putSymbol, attributesSymbol, name, actualValue)
}
