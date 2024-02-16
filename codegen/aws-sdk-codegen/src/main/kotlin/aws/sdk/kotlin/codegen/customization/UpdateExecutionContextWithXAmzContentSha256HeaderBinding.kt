/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.traits.HttpHeaderTrait

/**
 * Registers a middleware which overrides the execution context HashSpecification for members with an HTTP header binding
 * to `x-amz-content-sha256`.
 * https://github.com/awslabs/aws-sdk-kotlin/issues/1217
 */
class UpdateExecutionContextWithXAmzContentSha256HeaderBinding : KotlinIntegration {
    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware>  { return resolved + UpdateExecutionContextWithSha256HeaderBindingMiddleware() }
}

private class UpdateExecutionContextWithSha256HeaderBindingMiddleware : ProtocolMiddleware {
    override val name: String = "UpdateExecutionContextWithXAmzContentSha256HeaderBinding"

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = getXAmzContentSha256HeaderMember(ctx, op) != null

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val member = getXAmzContentSha256HeaderMember(ctx, op)!!
        writer.withBlock("input.${member.defaultName()}?.let {", "}") {
            writer.write("op.context[#1T.#2T] = #2T.Precalculated(it)", RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes, RuntimeTypes.Auth.Signing.AwsSigningCommon.HashSpecification)
        }
    }

    private fun getXAmzContentSha256HeaderMember(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): MemberShape? {
        val input = ctx.model.expectShape(op.inputShape)
        return input.members().singleOrNull { it.getTrait<HttpHeaderTrait>()?.value?.equals("x-amz-content-sha256") ?: false }
    }
}
