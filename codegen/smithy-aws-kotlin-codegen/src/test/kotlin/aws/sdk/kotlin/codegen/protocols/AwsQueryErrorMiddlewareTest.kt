/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import org.junit.jupiter.api.Test
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpTraitResolver
import software.amazon.smithy.kotlin.codegen.test.*

class AwsQueryErrorMiddlewareTest {
    @Test
    fun `it registers error deserializers for client error shapes`() {
        val model = """
            @http(method: "PUT", uri: "/test", code: 200)
            operation TestOperation {
                output: TestResponse,
                errors: [TestError]
            }
            
            structure TestResponse {
                someVal: Integer
            }
            
            @error("client")
            structure TestError {
                someCode: Integer,
                someMessage: String
            }
        """.prependNamespaceAndService(operations = listOf("TestOperation"))
            .toSmithyModel()

        val (ctx, _, _) = model.newTestContext()

        val unit = AwsQueryErrorMiddleware(ctx, HttpTraitResolver(ctx, "application/xml"))

        val actual = generateCode { writer ->
            unit.renderRegisterErrors(writer)
        }

        actual.shouldContainOnlyOnceWithDiff("""register(code = "TestError", deserializer = TestErrorDeserializer(), httpStatusCode = 400)""")
    }

    @Test
    fun `it registers error deserializers for server error shapes`() {
        val model = """
            @http(method: "PUT", uri: "/test", code: 200)
            operation TestOperation {
                output: TestResponse,
                errors: [TestError]
            }
            
            structure TestResponse {
                someVal: Integer
            }
            
            @error("server")
            structure TestError {
                someCode: Integer,
                someMessage: String
            }
        """.prependNamespaceAndService(operations = listOf("TestOperation"))
            .toSmithyModel()

        val (ctx, _, _) = model.newTestContext()

        val unit = AwsQueryErrorMiddleware(ctx, HttpTraitResolver(ctx, "application/xml"))

        val actual = generateCode { writer ->
            unit.renderRegisterErrors(writer)
        }

        actual.shouldContainOnlyOnceWithDiff("""register(code = "TestError", deserializer = TestErrorDeserializer(), httpStatusCode = 500)""")
    }

    @Test
    fun `it registers error deserializer with custom code for error shapes`() {
        val model = """
            @http(method: "PUT", uri: "/test", code: 200)
            operation TestOperation {
                output: TestResponse,
                errors: [TestError]
            }
            
            structure TestResponse {
                someVal: Integer
            }
            
            @error("client")
            @httpError(404)
            structure TestError {
                someCode: Integer,
                someMessage: String
            }
        """.prependNamespaceAndService(operations = listOf("TestOperation"))
            .toSmithyModel()

        val (ctx, _, _) = model.newTestContext()

        val unit = AwsQueryErrorMiddleware(ctx, HttpTraitResolver(ctx, "application/xml"))

        val actual = generateCode { writer ->
            unit.renderRegisterErrors(writer)
        }

        actual.shouldContainOnlyOnceWithDiff("""register(code = "TestError", deserializer = TestErrorDeserializer(), httpStatusCode = 404)""")
    }

    @Test
    fun `it registers correct code for awsQueryError trait`() {
        val model = """
          
            @http(method: "PUT", uri: "/test", code: 200)
            operation TestOperation {
                output: TestResponse,
                errors: [TestError]
            }
            
            structure TestResponse {
                someVal: Integer
            }
            
            @awsQueryError(
                code: "InvalidThing",
                httpResponseCode: 401,
            )
            @error("client")
            structure TestError {
                someCode: Integer,
                someMessage: String
            }
        """.prependNamespaceAndService(operations = listOf("TestOperation"), imports = listOf("aws.protocols#awsQueryError"))
            .toSmithyModel()

        val (ctx, _, _) = model.newTestContext()

        val unit = AwsQueryErrorMiddleware(ctx, HttpTraitResolver(ctx, "application/xml"))

        val actual = generateCode { writer ->
            unit.renderRegisterErrors(writer)
        }

        actual.shouldContainOnlyOnceWithDiff("""register(code = "InvalidThing", deserializer = TestErrorDeserializer(), httpStatusCode = 401)""")
    }
}
