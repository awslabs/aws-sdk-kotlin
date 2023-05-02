/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.hasIdempotentTokenMember
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Override for generating concrete (AWS) HTTP service clients
 */
open class AwsHttpProtocolClientGenerator(
    ctx: ProtocolGenerator.GenerationContext,
    middlewares: List<ProtocolMiddleware>,
    httpBindingResolver: HttpBindingResolver,
) : HttpProtocolClientGenerator(ctx, middlewares, httpBindingResolver) {

    override fun render(writer: KotlinWriter) {
        writer.write("\n\n")
        writer.write("public const val ServiceId: String = #S", ctx.service.sdkId)
        writer.write("public const val ServiceApiVersion: String = #S", ctx.service.version)
        writer.write("public const val SdkVersion: String = #S", ctx.settings.pkg.version)
        writer.write("\n\n")
        super.render(writer)
    }

    override fun renderInit(writer: KotlinWriter) {
        writer.withBlock("init {", "}") {
            write("managedResources.#T(config.httpClientEngine)", RuntimeTypes.Core.IO.addIfManaged)
            write("managedResources.#T(config.credentialsProvider)", RuntimeTypes.Core.IO.addIfManaged)
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
        val putIfAbsentSym = buildSymbol { name = "putIfAbsent"; namespace(KotlinDependency.CORE, "util") }
        val sdkClientOptionSym = buildSymbol { name = "SdkClientOption"; namespace(KotlinDependency.CORE, "client") }

        writer.dokka("merge the defaults configured for the service into the execution context before firing off a request")
        writer.withBlock(
            "private suspend fun mergeServiceDefaults(ctx: #T) {",
            "}",
            RuntimeTypes.Core.ExecutionContext,
        ) {
            write("ctx.#T(#T.Region, config.region)", putIfAbsentSym, AwsRuntimeTypes.Core.Client.AwsClientOption)
            write("ctx.#T(#T.ClientName, config.clientName)", putIfAbsentSym, sdkClientOptionSym)
            write("ctx.#T(#T.LogMode, config.logMode)", putIfAbsentSym, sdkClientOptionSym)
            // fill in auth/signing attributes
            if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.service)) {
                // default signing context (most of this has been moved to auth schemes but some things like event streams still depend on this)
                val signingServiceName = AwsSignatureVersion4.signingServiceName(ctx.service)
                write("ctx.#T(#T.SigningService, #S)", putIfAbsentSym, RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes, signingServiceName)
                write("ctx.#T(#T.SigningRegion, config.region)", putIfAbsentSym, RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes)
                write("ctx.#T(#T.CredentialsProvider, config.credentialsProvider)", putIfAbsentSym, RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes)
            }

            if (ctx.service.hasIdempotentTokenMember(ctx.model)) {
                write("config.idempotencyTokenProvider?.let { ctx[#T.IdempotencyTokenProvider] = it }", sdkClientOptionSym)
            }
        }
    }
}
