/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.route53

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

class CustomErrorUnmarshallingIntegration : KotlinIntegration {
    companion object {
        private val overrideCustomErrorUnmarshallingWriter = SectionWriter { writer, default ->
            val op = writer.getContextValue(HttpProtocolClientGenerator.OperationDeserializerBinding.Operation)
            if (op.id.name == "ChangeResourceRecordSets") {
                writer.write("deserializer = aws.sdk.kotlin.services.route53.internal.ChangeResourceRecordSetsOperationDeserializer()")
            } else {
                writer.write(default)
            }
        }
    }

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(SectionWriterBinding(HttpProtocolClientGenerator.OperationDeserializerBinding, overrideCustomErrorUnmarshallingWriter))

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("route 53", ignoreCase = true)
}
