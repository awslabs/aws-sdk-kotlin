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
            val lines = originalValue?.split("\n")
            writer.write("#L\naws.sdk.kotlin.services.route53.internal.parseCustomXmlErrorResponse(payload) ?: #L", lines?.get(0) ?: "", lines?.get(1) ?: "")
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
