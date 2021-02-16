/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.awsjson

import io.kotest.matchers.string.shouldContainOnlyOnce
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AwsJsonModeledExceptionsFeatureTest {

    @Test
    fun `it generates the registration of error deserializers for operations with errors`() {
        val testModel = """
            namespace smithy.example

            use aws.protocols#awsJson1_0

            @awsJson1_0
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {
                errors: [GetFooError]
            }

            @error("client")
            structure GetFooError {}
        """.asSmithyModel()

        val expected = """
            register(code = "GetFooError", deserializer = GetFooErrorDeserializer())
        """.trimIndent()

        val ctx = testModel.generateTestContext("smithy.example", "Example")

        val bindingResolver = AwsJsonHttpBindingResolver(ctx, "application/json")
        val unit = AwsJsonModeledExceptionsFeature(ctx, bindingResolver)

        val actual = generateCode(unit::renderConfigure)

        actual.shouldContainOnlyOnce(expected)
    }

    @Test
    fun `it generates nothing for operations without errors`() {
        val testModel = """
            namespace smithy.example

            use aws.protocols#awsJson1_0

            @awsJson1_0
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo { }
        """.asSmithyModel()

        val ctx = testModel.generateTestContext("smithy.example", "Example")
        val bindingResolver = AwsJsonHttpBindingResolver(ctx, "application/json")
        val unit = AwsJsonModeledExceptionsFeature(ctx, bindingResolver)

        val actual = generateCode(unit::renderConfigure)

        Assertions.assertTrue(actual.isEmpty())
    }
}
