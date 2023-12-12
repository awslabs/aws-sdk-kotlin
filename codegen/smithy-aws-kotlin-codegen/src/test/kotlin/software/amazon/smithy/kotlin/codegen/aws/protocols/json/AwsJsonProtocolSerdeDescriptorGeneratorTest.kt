/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.kotlin.codegen.aws.protocols.json

import software.amazon.smithy.kotlin.codegen.test.*
import software.amazon.smithy.model.shapes.ShapeId
import kotlin.test.Test

class AwsJsonProtocolSerdeDescriptorGeneratorTest {
    @Test
    fun itAddsIgnoreKeysTrait() {
        val model = """
                @http(method: "POST", uri: "/foo")
                operation Foo {
                    input: FooRequest
                }  
                
                structure FooRequest { 
                    strVal: String,
                    intVal: Integer
                }
                
                union Bar {
                    x: String,
                    y: String,
                }
        """.prependNamespaceAndService(operations = listOf("Foo")).toSmithyModel()

        val testCtx = model.newTestContext()
        val writer = testCtx.newWriter()
        val shape = model.expectShape(ShapeId.from("com.test#Bar"))
        val renderingCtx = testCtx.toRenderingContext(writer, shape)

        AwsJsonProtocolSerdeDescriptorGenerator(renderingCtx).render()

        val expectedDescriptors = """
                val X_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("x"))
                val Y_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("y"))
                val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                    trait(IgnoreKey("__type"))
                    field(X_DESCRIPTOR)
                    field(Y_DESCRIPTOR)
                }
            """.formatForTest("")

        val contents = writer.toString()
        contents.shouldContainOnlyOnceWithDiff(expectedDescriptors)
    }

    @Test
    fun itDoesNotAddIgnoreKeysTrait() {
        val model = """
                @http(method: "POST", uri: "/foo")
                operation Foo {
                    input: FooRequest
                }  
                
                structure FooRequest { 
                    strVal: String,
                    intVal: Integer
                }
                
                union Bar {
                    __type: String,
                    y: String,
                }
        """.prependNamespaceAndService(operations = listOf("Foo")).toSmithyModel()

        val testCtx = model.newTestContext()
        val writer = testCtx.newWriter()
        val shape = model.expectShape(ShapeId.from("com.test#Bar"))
        val renderingCtx = testCtx.toRenderingContext(writer, shape)

        AwsJsonProtocolSerdeDescriptorGenerator(renderingCtx).render()

        val expectedDescriptors = """
                val TYPE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("__type"))
                val Y_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("y"))
                val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                    field(TYPE_DESCRIPTOR)
                    field(Y_DESCRIPTOR)
                }
            """.formatForTest("")

        val contents = writer.toString()
        contents.shouldContainOnlyOnceWithDiff(expectedDescriptors)
    }
}
