/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.machinelearning

import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

class MachineLearningEndpointCustomization : KotlinIntegration {
    override val order: Byte
        get() = super.order

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId.equals("Machine Learning", ignoreCase = true)

    override val sectionWriters: List<SectionWriterBinding> =
        listOf(SectionWriterBinding(HttpProtocolClientGenerator.OperationSerializerBinding, customizeSerializerWriter))
}

private val customizeSerializerWriter = SectionWriter { writer, default ->
    val op: OperationShape = checkNotNull(writer.getContext(HttpProtocolClientGenerator.OperationSerializerBinding.Operation) as OperationShape?) {
        "Expected ${HttpProtocolClientGenerator.OperationSerializerBinding.Operation} key in context."
    }

    if (op.id.name == "Predict") {
        writer.write(
            "serializer = #L.#L",
            "aws.sdk.kotlin.services.machinelearning.internal",
            "EndpointCustomizingPredictOperationSerializer()"
        )
    } else {
        writer.write(default)
    }
}
