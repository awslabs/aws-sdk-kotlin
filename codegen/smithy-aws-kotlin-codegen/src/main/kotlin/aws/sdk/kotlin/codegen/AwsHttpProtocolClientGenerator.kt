/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.*
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Override for generating concrete (AWS) HTTP service clients
 */
class AwsHttpProtocolClientGenerator(
    ctx: ProtocolGenerator.GenerationContext,
    features: List<HttpFeature>,
    httpBindingResolver: HttpBindingResolver
) : HttpProtocolClientGenerator(ctx, features, httpBindingResolver) {

    override val serdeProviderSymbol: Symbol
        get() {
            return when (ctx.protocol) {
                AwsJson1_1Trait.ID,
                AwsJson1_0Trait.ID,
                RestJson1Trait.ID -> buildSymbol {
                    name = "JsonSerdeProvider"
                    namespace(KotlinDependency.CLIENT_RT_SERDE_JSON)
                }
                else -> throw CodegenException("no serialization provider implemented for: ${ctx.protocol}")
            }
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
            writer.addImport("AuthAttributes", AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
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
        writer.addImport("ExecutionContext", KotlinDependency.CLIENT_RT_CORE, "${KotlinDependency.CLIENT_RT_CORE.namespace}.client")
        writer.addImport("SdkClientOption", KotlinDependency.CLIENT_RT_CORE, "${KotlinDependency.CLIENT_RT_CORE.namespace}.client")
        writer.addImport("resolveRegionForOperation", AwsKotlinDependency.AWS_CLIENT_RT_REGIONS)
        writer.addImport("AuthAttributes", AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
        writer.addImport(
            "AwsClientOption",
            AwsKotlinDependency.AWS_CLIENT_RT_CORE, "${AwsKotlinDependency.AWS_CLIENT_RT_CORE.namespace}.client"
        )
        writer.addImport("putIfAbsent", KotlinDependency.CLIENT_RT_UTILS)

        writer.dokka("merge the defaults configured for the service into the execution context before firing off a request")
        writer.openBlock("private fun mergeServiceDefaults(ctx: ExecutionContext) {", "}") {
            writer.write("val region = resolveRegionForOperation(ctx, config)")
            writer.write("ctx.putIfAbsent(AwsClientOption.Region, region)")
            writer.write("ctx.putIfAbsent(AuthAttributes.SigningRegion, config.signingRegion ?: region)")
            writer.write("ctx.putIfAbsent(SdkClientOption.ServiceName, serviceName)")

            if (ctx.service.hasIdempotentTokenMember(ctx.model)) {
                writer.addImport(RuntimeTypes.Core.IdempotencyTokenProviderExt)
                writer.write("config.idempotencyTokenProvider?.let { ctx[SdkClientOption.IdempotencyTokenProvider] = it }")
            }
        }
    }

    private fun renderInternals() {
        val endpointData = Node.parse(
            EndpointResolverGenerator::class.java.getResource("endpoints.json").readText()
        ).expectObjectNode()
        EndpointResolverGenerator(endpointData).render(ctx)
    }
}
