/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.route53

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.aws.protocols.core.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId

class ChangeResourceRecordSetsUnmarshallingIntegration : KotlinIntegration {
    private val targetErrorShapeId = ShapeId.from("com.amazonaws.route53#InvalidChangeBatch")

    override val sectionWriters: List<SectionWriterBinding> = listOf(
        SectionWriterBinding(AwsHttpBindingProtocolGenerator.Sections.ProtocolErrorDeserialization) { writer, prevValue ->
            val op = writer.getContextValue(AwsHttpBindingProtocolGenerator.Sections.RenderThrowOperationError.Operation)

            if (op.errors.any { it == targetErrorShapeId }) {
                writer.withBlock("payload?.let {", "}") {
                    withBlock("aws.sdk.kotlin.services.route53.internal.parseRestXmlInvalidChangeBatchResponse(payload)?.let {", "}") {
                        write("setAseErrorMetadata(it.exception, wrappedResponse, it.errorDetails)")
                        write("throw it.exception")
                    }
                }
                writer.write("")
            } else {
                writer.write(prevValue)
            }
        },
    )

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("route 53", ignoreCase = true)
}
