/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.glacier

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*

/**
 * Adds a middleware for Glacier to autofill accountId when not set
 * See: https://github.com/awslabs/aws-sdk-kotlin/issues/246
 */
class GlacierAccountIdDefault : KotlinIntegration {
    override val order: Byte = -127
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("Glacier", ignoreCase = true)

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> {
        return resolved + GlacierAccountIdMiddleware()
    }
}

private class GlacierAccountIdMiddleware : ProtocolMiddleware {
    override val name: String
        get() = "GlacierAccountIdAutoFill"

    override val order: Byte = -127

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
        val input = op.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }
        return input?.memberNames?.any { it.lowercase() == "accountid" } ?: false
    }

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val accountId = ctx.model.expectShape<StructureShape>(op.input.get()).members().first { it.memberName.lowercase() == "accountid" }
        writer.addImport(RuntimeTypes.Http.Operation.OperationRequest)

        writer.withBlock("execution.initialize.intercept { req, next -> ", "}") {
            write("if (req.subject.#L.isNullOrEmpty()) {", accountId.defaultName())
                .indent()
                .write("val updated = req.subject.copy { #L = #S }", accountId.defaultName(), "-")
                .write("next.call(#T(req.context, updated))", RuntimeTypes.Http.Operation.OperationRequest)
                .dedent()
                .write("} else {")
                .indent()
                .write("next.call(req)")
                .dedent()
                .write("}")
        }
    }
}
