/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.sqs

import aws.sdk.kotlin.codegen.ServiceClientCompanionObjectWriter
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Register interceptor to handle SQS message MD5 checksum validation.
 */
class SqsMd5ChecksumValidationIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isSqs

    companion object {
        val ValidationEnabledProp: ConfigProperty = ConfigProperty {
            name = "checksumValidationEnabled"
            symbol = buildSymbol {
                name = "ValidationEnabled"
                namespace = "aws.sdk.kotlin.services.sqs.internal"
            }
            documentation = """
                    Specifies when MD5 checksum validation should be performed for SQS messages. This controls the automatic 
                    calculation and validation of checksums during message operations.
                    
                    Valid values:
                    - `ALWAYS` (default) - Checksums are calculated and validated for both sending and receiving operations 
                      (SendMessage, SendMessageBatch, and ReceiveMessage)
                    - `WHEN_SENDING` - Checksums are only calculated and validated during send operations 
                      (SendMessage and SendMessageBatch)
                    - `WHEN_RECEIVING` - Checksums are only calculated and validated during receive operations 
                      (ReceiveMessage)
                    - `NEVER` - No checksum calculation or validation is performed
                """.trimIndent()
        }

        private val validationScope = buildSymbol {
            name = "ValidationScope"
            namespace = "aws.sdk.kotlin.services.sqs.internal"
        }

        val ValidationScopeProp: ConfigProperty = ConfigProperty {
            name = "checksumValidationScopes"
            symbol = KotlinTypes.Collections.set(validationScope, default = "emptySet()")
            documentation = """
                Specifies which parts of an SQS message should undergo MD5 checksum validation. This configuration 
                accepts a set of validation scopes that determine which message components to validate.
                
                Valid values:
                - `MESSAGE_ATTRIBUTES` - Validates checksums for message attributes
                - `MESSAGE_SYSTEM_ATTRIBUTES` - Validates checksums for message system attributes 
                  (Note: Not available for ReceiveMessage operations as SQS does not calculate checksums for 
                  system attributes during message receipt)
                - `MESSAGE_BODY` - Validates checksums for the message body
                
                Default: All three scopes (MESSAGE_ATTRIBUTES, MESSAGE_SYSTEM_ATTRIBUTES, MESSAGE_BODY)
            """.trimIndent()
        }
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> =
        listOf(
            ValidationEnabledProp,
            ValidationScopeProp,
        )

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(
                ServiceClientCompanionObjectWriter.FinalizeEnvironmentalConfig,
                finalizeSqsConfigWriter,
            ),
        )

    // add Sqs-specific config finalization
    private val finalizeSqsConfigWriter = AppendingSectionWriter { writer ->
        val finalizeSqsConfig = buildSymbol {
            name = "finalizeSqsConfig"
            namespace = "aws.sdk.kotlin.services.sqs.internal"
        }
        writer.write("#T(builder, sharedConfig)", finalizeSqsConfig)
    }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>,
    ): List<ProtocolMiddleware> = resolved + listOf(SqsMd5ChecksumValidationMiddleware)
}

internal object SqsMd5ChecksumValidationMiddleware : ProtocolMiddleware {
    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = when (op.id.name) {
        "ReceiveMessage",
        "SendMessage",
        "SendMessageBatch",
        -> true
        else -> false
    }

    override val name: String = "SqsMd5ChecksumValidationInterceptor"

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        val symbol = buildSymbol {
            name = this@SqsMd5ChecksumValidationMiddleware.name
            namespace = "aws.sdk.kotlin.services.sqs"
        }

        writer.write("op.interceptors.add(#T(config.checksumValidationEnabled, config.checksumValidationScopes))", symbol)
    }
}
