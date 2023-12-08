/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.transforms

import software.amazon.smithy.model.shapes.FloatShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.DeprecatedTrait
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveDeprecatedShapesTest {
    private fun shapeDeprecatedSince(since: String?): Shape {
        val deprecatedTrait = DeprecatedTrait.builder()
            .since(since)
            .build()

        return FloatShape.builder()
            .addTrait(deprecatedTrait)
            .id(ShapeId.from("aws.sdk.kotlin#testing"))
            .build()
    }

    @Test
    fun testShouldBeRemoved() {
        val shape = shapeDeprecatedSince("2022-01-01")
        val removeDeprecatedShapesUntil = "2023-01-01".toLocalDate()!!
        assertTrue(shouldRemoveDeprecatedShape(removeDeprecatedShapesUntil).test(shape))
    }

    @Test
    fun testShouldNotBeRemoved() {
        val shape = shapeDeprecatedSince("2024-01-01")
        val removeDeprecatedShapesUntil = "2023-01-01".toLocalDate()!!
        assertFalse(shouldRemoveDeprecatedShape(removeDeprecatedShapesUntil).test(shape))
    }

    @Test
    fun testShouldNotBeRemovedIfDeprecatedSameDay() {
        val shape = shapeDeprecatedSince("2023-01-01")
        val removeDeprecatedShapesUntil = "2023-01-01".toLocalDate()!!
        assertFalse(shouldRemoveDeprecatedShape(removeDeprecatedShapesUntil).test(shape))
    }

    @Test
    fun testShouldNotBeRemovedIfMissingSinceField() {
        val shape = shapeDeprecatedSince(null)
        val removeDeprecatedShapesUntil = "2023-01-01".toLocalDate()!!
        assertFalse(shouldRemoveDeprecatedShape(removeDeprecatedShapesUntil).test(shape))
    }
}
