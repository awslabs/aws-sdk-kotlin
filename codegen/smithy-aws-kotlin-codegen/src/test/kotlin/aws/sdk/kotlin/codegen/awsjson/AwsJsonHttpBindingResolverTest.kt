/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.awsjson

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import software.amazon.smithy.kotlin.codegen.expectShape
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape

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
        """.asSmithyModel()

    @Test
    fun `it resolves all operations associated with a service`() {
        val ctx = testModel.generateTestContext("smithy.example", "Example")

        val unit = AwsJsonHttpBindingResolver(ctx, "application/json")

        val expectedOperations = listOf("GetEmptyFoo", "GetFoo")
        val actualOperations = unit.bindingOperations().map { operationShape -> operationShape.id.name }.sorted()

        Assertions.assertEquals(expectedOperations, actualOperations)
    }

    @Test
    fun `it returns no request bindings for operations without inputs`() {
        val ctx = testModel.generateTestContext("smithy.example", "Example")
        val operation = testModel.expectShape<OperationShape>("smithy.example#GetEmptyFoo")
        val unit = AwsJsonHttpBindingResolver(ctx, "application/json")

        val actualRequestBindings = unit.requestBindings(operation)

        Assertions.assertTrue(actualRequestBindings.isEmpty())
    }

    @Test
    fun `it returns request bindings for operations with inputs`() {
        val ctx = testModel.generateTestContext("smithy.example", "Example")
        val operation = testModel.expectShape<OperationShape>("smithy.example#GetFoo")
        val unit = AwsJsonHttpBindingResolver(ctx, "application/json")

        val actualRequestBindings = unit.requestBindings(operation)

        Assertions.assertTrue(actualRequestBindings.size == 1)
        val binding = actualRequestBindings.first()

        Assertions.assertEquals(binding.member.id.toString(), "smithy.example#GetFooInput\$bigInt")
        Assertions.assertEquals(binding.location, HttpBinding.Location.DOCUMENT)
        // Location name is unused by awsJson
        Assertions.assertEquals(binding.locationName, "")
    }

    @Test
    fun `it returns no response bindings for operations without inputs`() {
        val ctx = testModel.generateTestContext("smithy.example", "Example")
        val operation = testModel.expectShape<OperationShape>("smithy.example#GetEmptyFoo")
        val unit = AwsJsonHttpBindingResolver(ctx, "application/json")

        val actualResponseBindings = unit.responseBindings(operation)

        Assertions.assertTrue(actualResponseBindings.isEmpty())
    }

    @Test
    fun `it returns response bindings for operations with inputs`() {
        val ctx = testModel.generateTestContext("smithy.example", "Example")
        val operation = testModel.expectShape<OperationShape>("smithy.example#GetFoo")
        val unit = AwsJsonHttpBindingResolver(ctx, "application/json")

        val actualResponseBindings = unit.responseBindings(operation)

        Assertions.assertTrue(actualResponseBindings.size == 1)
        val binding = actualResponseBindings.first()

        Assertions.assertEquals(binding.member.id.toString(), "smithy.example#GetFooOutput\$bigInt")
        Assertions.assertEquals(binding.location, HttpBinding.Location.DOCUMENT)
        // Location name is unused by awsJson
        Assertions.assertEquals(binding.locationName, "")
    }

    @Test
    fun `it returns no response bindings for structures without members`() {
        val ctx = testModel.generateTestContext("smithy.example", "Example")
        val structure = testModel.expectShape<StructureShape>("smithy.example#GetFooError")
        val unit = AwsJsonHttpBindingResolver(ctx, "application/json")

        val actualResponseBindings = unit.responseBindings(structure)

        Assertions.assertTrue(actualResponseBindings.isEmpty())
    }

    @Test
    fun `it returns response bindings for structures with members`() {
        val ctx = testModel.generateTestContext("smithy.example", "Example")
        val structure = testModel.expectShape<StructureShape>("smithy.example#GetFooOutput")
        val unit = AwsJsonHttpBindingResolver(ctx, "application/json")

        val actualResponseBindings = unit.responseBindings(structure)

        Assertions.assertTrue(actualResponseBindings.size == 1)
        val binding = actualResponseBindings.first()

        Assertions.assertEquals(binding.member.id.toString(), "smithy.example#GetFooOutput\$bigInt")
        Assertions.assertEquals(binding.location, HttpBinding.Location.DOCUMENT)
        // Location name is unused by awsJson
        Assertions.assertEquals(binding.locationName, "")
    }
}
