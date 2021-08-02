/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization

import org.junit.jupiter.api.Test
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.prependNamespaceAndService
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.BoxTrait
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoxServicesTest {
    @Test
    fun testPrimitiveShapesAreBoxed() {
        val model = """
            structure Primitives {
                int: PrimitiveInteger,
                bool: PrimitiveBoolean,
                long: PrimitiveLong,
                double: PrimitiveDouble,
                boxedAlready: BoxedField,
                other: Other
            }
            @box
            integer BoxedField
            structure Other {}
        """.prependNamespaceAndService().toSmithyModel()

        val ctx = model.newTestContext()
        val transformed = BoxServices().preprocessModel(model, ctx.generationCtx.settings)

        val struct = transformed.expectShape<StructureShape>("com.test#Primitives")
        struct.members().forEach {
            when (val target = transformed.expectShape(it.target)) {
                is StructureShape -> assertFalse(target.hasTrait<BoxTrait>())
                else -> assertTrue(target.hasTrait<BoxTrait>())
            }
        }
    }
}
