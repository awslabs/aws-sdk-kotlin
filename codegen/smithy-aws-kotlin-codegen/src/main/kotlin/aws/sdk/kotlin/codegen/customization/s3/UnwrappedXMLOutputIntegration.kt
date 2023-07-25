/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.traits.UnwrappedXMLOutput
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.transform.ModelTransformer

private val SHAPE_IDS = setOf(
    "com.amazonaws.s3#GetBucketLocationOutput",
)

/**
 * Applies the [UnwrappedXMLOutput] custom-made [annotation trait](https://smithy.io/2.0/spec/model.html?highlight=annotation#annotation-traits) to a manually-curated list of structures
 * to mark when special unwrapped xml output deserialization is required.
 */
class UnwrappedXMLOutputIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model = ModelTransformer
        .create()
        .mapShapes(model) { shape ->
            when {
                shape.id.toString() in SHAPE_IDS -> {
                    check(shape is StructureShape) { "Cannot apply UnwrappedXMLOutput to non-member shape" }
                    shape.toBuilder().addTrait(UnwrappedXMLOutput()).build()
                }
                else -> shape
            }
        }
}
