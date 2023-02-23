/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Registers an integration that replaces the codegened GetBucketLocationDeserializer with
 * custom deserialization logic.
 */
class GetBucketLocationDeserializerIntegration : KotlinIntegration {
    companion object {
        // For the S3 GetBucketLocation operation, substitute the codegened deserializer for a handwritten variant.
        // In the future there may be a specific trait to address this specific issue.  See https://github.com/awslabs/smithy/pull/839.
        private val overrideGetBucketLocationDeserializerWriter = SectionWriter { writer, default ->
            val op = writer.getContextValue(HttpProtocolClientGenerator.OperationDeserializerBinding.Operation)
            if (op.id.name == "GetBucketLocation") {
                writer.write("deserializer = aws.sdk.kotlin.services.s3.internal.GetBucketLocationOperationDeserializer()")
            } else {
                writer.write(default)
            }
        }
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(SectionWriterBinding(HttpProtocolClientGenerator.OperationDeserializerBinding, overrideGetBucketLocationDeserializerWriter))

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3
}
