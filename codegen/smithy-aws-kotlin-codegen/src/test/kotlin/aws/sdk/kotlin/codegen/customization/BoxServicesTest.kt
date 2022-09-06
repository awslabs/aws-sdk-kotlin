/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.isNumberShape
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.prependNamespaceAndService
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.StructureShape
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoxServicesTest {
    @Test
    fun testPrimitiveShapesAreBoxed() {
        val model = """
            operation Foo {
                input: Primitives
            }
            
            structure Primitives {
                int: PrimitiveInteger,
                bool: PrimitiveBoolean,
                long: PrimitiveLong,
                double: PrimitiveDouble,
                boxedAlready: BoxedField,
                notBoxed: NotBoxedField,
                other: Other
            }
            
            @box
            integer BoxedField
            
            structure Other {}
            
            integer NotBoxedField
        """.prependNamespaceAndService(operations = listOf("Foo")).toSmithyModel()

        val ctx = model.newTestContext()
        val transformed = BoxServices().preprocessModel(model, ctx.generationCtx.settings)

        // get the synthetic input which is the one that will be transformed
        val struct = transformed.expectShape<StructureShape>("smithy.kotlin.synthetic.test#FooRequest")
        struct.members().forEach {
            val target = transformed.expectShape(it.target)
            if (target.isBooleanShape || target.isNumberShape) {
                assertTrue(it.hasTrait<@Suppress("DEPRECATION") software.amazon.smithy.model.traits.BoxTrait>())
            } else {
                assertFalse(target.hasTrait<@Suppress("DEPRECATION") software.amazon.smithy.model.traits.BoxTrait>())
                assertFalse(it.hasTrait<@Suppress("DEPRECATION") software.amazon.smithy.model.traits.BoxTrait>())
            }
        }
    }
}
