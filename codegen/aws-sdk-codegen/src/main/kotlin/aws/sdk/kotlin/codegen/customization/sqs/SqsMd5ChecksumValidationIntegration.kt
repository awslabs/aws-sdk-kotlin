/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.sqs

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Register interceptor to handle SQS message MD5 checksum validation.
 */
class SqsMd5ChecksumValidationIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isSqs

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + listOf(SqsMd5ChecksumValidationMiddleware)
}

internal object SqsMd5ChecksumValidationMiddleware : ProtocolMiddleware {
    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
        return when (op.id.name) {
            "ReceiveMessage",
            "SendMessage",
            "SendMessageBatch" -> true
            else -> false
        }
    }

    override val name: String = "SqsMd5ChecksumValidationInterceptor"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val symbol = buildSymbol {
            name = this@SqsMd5ChecksumValidationMiddleware.name
            namespace = "aws.sdk.kotlin.services.sqs"
        }

        writer.write("op.interceptors.add(#T(config.checksumValidationEnabled, config.checksumValidationScopes,))", symbol)
    }
}
