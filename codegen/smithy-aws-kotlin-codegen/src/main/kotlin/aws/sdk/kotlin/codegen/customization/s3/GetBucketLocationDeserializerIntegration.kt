/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingProtocolGenerator.payloadDeserializer
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Registers an integration that tries deserializing the `GetBucketLocationOperation`
 * response as wrapped XML response when doing so as [unwrapped](https://smithy.io/2.0/aws/customizations/s3-customizations.html#aws-customizations-s3unwrappedxmloutput-trait) is unsuccessful.
 */
class GetBucketLocationDeserializerIntegration : KotlinIntegration {
    override val sectionWriters: List<SectionWriterBinding> = listOf(
        SectionWriterBinding(payloadDeserializer) { writer, default ->
            if (isGetBucketLocation(writer)) {
                writer.write("if (builder.locationConstraint == null) aws.sdk.kotlin.services.s3.internal.deserializeWrapped(builder, payload)")
            } else {
                writer.write(default)
            }
        },
    )

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3
}

private fun isGetBucketLocation(writer: KotlinWriter): Boolean {
    val operation: String = writer.getContext("operation") as String
    check(operation.isNotBlank()) { "Expected ${payloadDeserializer.operation} key in context" }
    return operation == "deserializeGetBucketLocationOperationBody"
}
