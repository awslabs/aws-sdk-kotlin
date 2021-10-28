/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.glacier

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.addImport
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.isStreaming
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpFeatureMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape

public class GlacierBodyChecksum : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("Glacier", ignoreCase = true)

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + glacierBodyChecksumMiddleware

    private val glacierBodyChecksumMiddleware = object : HttpFeatureMiddleware() {
        override val order: Byte = 127 // Must come after AwsSignatureVersion4
        override val name: String = "GlacierBodyChecksum"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
            val input = op.input.getOrNull()?.let { ctx.model.expectShape<StructureShape>(it) }
            return (input?.members()?.any { it.isStreaming || ctx.model.expectShape(it.target).isStreaming } == true).also {
                println("GlacierBodyChecksum enabled for ${op.defaultName()}? $it")
            }
        }

        override fun renderConfigure(writer: KotlinWriter) {
            writer.addImport(AwsRuntimeTypes.Http.Middleware.GlacierBodyChecksum)
        }
    }
}
