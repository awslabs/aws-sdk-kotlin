/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.route53

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.transform.ModelTransformer

class ChangeResourceRecordSetsUnmarshallingIntegration : KotlinIntegration {
    override val sectionWriters: List<SectionWriterBinding> = listOf(
        SectionWriterBinding(AwsHttpBindingProtocolGenerator.ProtocolErrorDeserialization) { writer, originalValue ->
            requireNotNull(originalValue) { "Unexpectedly empty original value" }
            val (nullCheckLine, parseLine) = originalValue.split("\n", limit = 2)
            writer.write("#L", nullCheckLine)
            writer.write("aws.sdk.kotlin.services.route53.internal.parseCustomXmlErrorResponse(payload) ?: #L", parseLine)
        },
    )

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()
        return transformer.mapShapes(model) { shape ->
            if (shape.id.name == "ChangeResourceRecordsSet" && shape is OperationShape) {
                shape.errors.removeIf { error -> error.name == "InvalidChangeBatch" }
            }
            shape
        }
    }

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("route 53", ignoreCase = true)
}
