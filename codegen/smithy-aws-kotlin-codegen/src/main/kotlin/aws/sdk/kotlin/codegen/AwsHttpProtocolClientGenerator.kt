/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait
import software.amazon.smithy.kotlin.codegen.KotlinDependency
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.addImport
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.knowledge.OperationIndex
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Override for generating concrete (AWS) HTTP service clients
 */
class AwsHttpProtocolClientGenerator(
    ctx: ProtocolGenerator.GenerationContext,
    rootNamespace: String,
    features: List<HttpFeature>
) : HttpProtocolClientGenerator(ctx, rootNamespace, features) {

    override fun renderOperationSetup(writer: KotlinWriter, opIndex: OperationIndex, op: OperationShape) {
        super.renderOperationSetup(writer, opIndex, op)

        // add in additional context and defaults
        if (op.hasTrait(UnsignedPayloadTrait::class.java)) {
            writer.addImport("AuthAttributes", AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
            writer.write("execCtx[AuthAttributes.UnsignedPayload] = true")
        }

        writer.write("mergeServiceDefaults(execCtx)")
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
        }
    }
}
