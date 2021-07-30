/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.KotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.addImport
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.hasIdempotentTokenMember
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Override for generating concrete (AWS) HTTP service clients
 */
open class AwsHttpProtocolClientGenerator(
    ctx: ProtocolGenerator.GenerationContext,
    middlewares: List<ProtocolMiddleware>,
    httpBindingResolver: HttpBindingResolver
) : HttpProtocolClientGenerator(ctx, middlewares, httpBindingResolver) {

    override val defaultHttpClientEngineSymbol: Symbol
        get() = buildSymbol {
            name = "CrtHttpEngine"
            namespace(AwsKotlinDependency.AWS_CRT_HTTP_ENGINE)
        }

    override fun render(writer: KotlinWriter) {
        writer.write("\n\n")
        writer.write("const val ServiceId: String = #S", ctx.service.sdkId)
        writer.write("const val ServiceApiVersion: String = #S", ctx.service.version)
        writer.write("const val SdkVersion: String = #S", ctx.settings.pkg.version)
        writer.write("\n\n")
        super.render(writer)

        // render internal files used by the implementation
        renderInternals()
    }

    override fun renderOperationSetup(writer: KotlinWriter, opIndex: OperationIndex, op: OperationShape) {
        super.renderOperationSetup(writer, opIndex, op)

        // add in additional context and defaults
        if (op.hasTrait(UnsignedPayloadTrait::class.java)) {
            writer.addImport(AwsRuntimeTypes.Core.AuthAttributes)
            writer.write("op.context[AuthAttributes.UnsignedPayload] = true")
        }

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
        writer.addImport(RuntimeTypes.Core.ExecutionContext)
        writer.addImport("SdkClientOption", KotlinDependency.CORE, "${KotlinDependency.CORE.namespace}.client")
        writer.addImport("resolveRegionForOperation", AwsKotlinDependency.AWS_REGIONS)
        writer.addImport(AwsRuntimeTypes.Core.AuthAttributes)
        writer.addImport(AwsRuntimeTypes.Core.AwsClientOption)
        writer.addImport("putIfAbsent", KotlinDependency.UTILS)

        writer.dokka("merge the defaults configured for the service into the execution context before firing off a request")
        writer.openBlock("private suspend fun mergeServiceDefaults(ctx: ExecutionContext) {", "}") {
            writer.write("val region = resolveRegionForOperation(ctx, config)")
            writer.write("ctx.putIfAbsent(AwsClientOption.Region, region)")
            writer.write("ctx.putIfAbsent(AuthAttributes.SigningRegion, config.signingRegion ?: region)")
            writer.write("ctx.putIfAbsent(SdkClientOption.ServiceName, serviceName)")
            writer.write("ctx.putIfAbsent(SdkClientOption.LogMode, config.sdkLogMode)")

            if (ctx.service.hasIdempotentTokenMember(ctx.model)) {
                writer.addImport(RuntimeTypes.Core.IdempotencyTokenProviderExt)
                writer.write("config.idempotencyTokenProvider?.let { ctx[SdkClientOption.IdempotencyTokenProvider] = it }")
            }
        }
    }

    private fun renderInternals() {
        val endpointsData = javaClass.classLoader.getResource("aws/sdk/kotlin/codegen/endpoints.json")?.readText() ?: throw CodegenException("could not load endpoints.json resource")
        val endpointData = Node.parse(endpointsData).expectObjectNode()
        EndpointResolverGenerator(endpointData).render(ctx)
    }
}
