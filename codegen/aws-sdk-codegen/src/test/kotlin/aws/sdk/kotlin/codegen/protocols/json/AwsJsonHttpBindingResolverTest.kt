/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.json

import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AwsJsonHttpBindingResolverTest {
    private val testModel = """
            namespace smithy.example

            use aws.protocols#awsJson1_0

            @awsJson1_0
            service Example {
                version: "1.0.0",
                operations: [GetEmptyFoo, GetFoo]
            }

            operation GetEmptyFoo { }

            operation GetFoo {
                input: GetFooInput,
                output: GetFooOutput,
                errors: [GetFooError]
            }

            structure GetFooInput {
                bigInt: BigInteger
            }
            structure GetFooOutput {
                bigInt: BigInteger
            }

            @error("client")
            structure GetFooError {}
        """.toSmithyModel(applyDefaultTransforms = false)

    @Test
    fun `it resolves all operations associated with a service`() {
        val (ctx, _, _) = testModel.newTestContext("Example", "smithy.example")

        val unit = AwsJsonHttpBindingResolver(ctx.model, ctx.service, "application/json")

        val expectedOperations = listOf("GetEmptyFoo", "GetFoo")
        val actualOperations = unit.bindingOperations().map { operationShape -> operationShape.id.name }.sorted()

        assertEquals(expectedOperations, actualOperations)
    }

    @Test
    fun `it returns no request bindings for operations without inputs`() {
        val (ctx, _, _) = testModel.newTestContext("Example", "smithy.example")
        val operation = testModel.expectShape<OperationShape>("smithy.example#GetEmptyFoo")
        val unit = AwsJsonHttpBindingResolver(ctx.model, ctx.service, "application/json")

        val actualRequestBindings = unit.requestBindings(operation)

        assertTrue(actualRequestBindings.isEmpty())
    }

    @Test
    fun `it returns request bindings for operations with inputs`() {
        val (ctx, _, _) = testModel.newTestContext("Example", "smithy.example")
        val operation = testModel.expectShape<OperationShape>("smithy.example#GetFoo")
        val unit = AwsJsonHttpBindingResolver(ctx.model, ctx.service, "application/json")

        val actualRequestBindings = unit.requestBindings(operation)

        assertTrue(actualRequestBindings.size == 1)
        val binding = actualRequestBindings.first()

        assertEquals(binding.member.id.toString(), "smithy.example#GetFooInput\$bigInt")
        assertEquals(binding.location, HttpBinding.Location.DOCUMENT)
        // Location name is unused by awsJson
        assertEquals(binding.locationName, null)
    }

    @Test
    fun `it returns no response bindings for operations without inputs`() {
        val (ctx, _, _) = testModel.newTestContext("Example", "smithy.example")
        val operation = testModel.expectShape<OperationShape>("smithy.example#GetEmptyFoo")
        val unit = AwsJsonHttpBindingResolver(ctx.model, ctx.service, "application/json")

        val actualResponseBindings = unit.responseBindings(operation)

        assertTrue(actualResponseBindings.isEmpty())
    }

    @Test
    fun `it returns response bindings for operations with inputs`() {
        val (ctx, _, _) = testModel.newTestContext("Example", "smithy.example")
        val operation = testModel.expectShape<OperationShape>("smithy.example#GetFoo")
        val unit = AwsJsonHttpBindingResolver(ctx.model, ctx.service, "application/json")

        val actualResponseBindings = unit.responseBindings(operation)

        assertTrue(actualResponseBindings.size == 1)
        val binding = actualResponseBindings.first()

        assertEquals(binding.member.id.toString(), "smithy.example#GetFooOutput\$bigInt")
        assertEquals(binding.location, HttpBinding.Location.DOCUMENT)
        // Location name is unused by awsJson
        assertEquals(binding.locationName, null)
    }

    @Test
    fun `it returns no response bindings for structures without members`() {
        val (ctx, _, _) = testModel.newTestContext("Example", "smithy.example")
        val structure = testModel.expectShape<StructureShape>("smithy.example#GetFooError")
        val unit = AwsJsonHttpBindingResolver(ctx.model, ctx.service, "application/json")

        val actualResponseBindings = unit.responseBindings(structure)

        assertTrue(actualResponseBindings.isEmpty())
    }

    @Test
    fun `it returns response bindings for structures with members`() {
        val (ctx, _, _) = testModel.newTestContext("Example", "smithy.example")
        val structure = testModel.expectShape<StructureShape>("smithy.example#GetFooOutput")
        val unit = AwsJsonHttpBindingResolver(ctx.model, ctx.service, "application/json")

        val actualResponseBindings = unit.responseBindings(structure)

        assertTrue(actualResponseBindings.size == 1)
        val binding = actualResponseBindings.first()

        assertEquals(binding.member.id.toString(), "smithy.example#GetFooOutput\$bigInt")
        assertEquals(binding.location, HttpBinding.Location.DOCUMENT)
        // Location name is unused by awsJson
        assertEquals(binding.locationName, null)
    }
}
