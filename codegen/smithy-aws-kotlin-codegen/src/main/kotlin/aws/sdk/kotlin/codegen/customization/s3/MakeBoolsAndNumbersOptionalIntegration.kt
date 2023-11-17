/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.AbstractShapeBuilder
import software.amazon.smithy.model.shapes.BooleanShape
import software.amazon.smithy.model.shapes.NumberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.DefaultTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * The s3 model is being updated to make this change, but this does it preemptively to avoid breaking changes
 * when that occurs.
 */
class MakeBoolsAndNumbersOptionalIntegration : KotlinIntegration {

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val updates = arrayListOf<Shape>()
        for (struct in model.structureShapes) {
            for (member in struct.allMembers.values) {
                val target = model.expectShape(member.target)
                val boolTarget = target as? BooleanShape
                val numberTarget = target as? NumberShape
                if (boolTarget != null || numberTarget != null) {
                    updates.add(member.toBuilder().removeTrait(DefaultTrait.ID).build())
                    val builder: AbstractShapeBuilder<*, *> = Shape.shapeToBuilder(target)
                    updates.add(builder.removeTrait(DefaultTrait.ID).build())
                }
            }
        }
        return ModelTransformer.create().replaceShapes(model, updates)
    }
}
