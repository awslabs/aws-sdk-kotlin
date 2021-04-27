/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.json

import aws.sdk.kotlin.codegen.protocols.AwsJson1_0
import aws.sdk.kotlin.codegen.protocols.AwsJson1_1
import aws.sdk.kotlin.codegen.protocols.RestJson1
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.test.codegenTestHarnessForModelSnippet
import software.amazon.smithy.kotlin.codegen.test.formatForTest
import software.amazon.smithy.kotlin.codegen.test.generateDeSerializers
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff

/**
 * This class exercises serde field and object descriptor generation for awsJson and restJson protocols.
 */
class AwsJsonFieldObjectDescriptorTest {

    @ParameterizedTest
    @ValueSource(classes = [AwsJson1_0::class, AwsJson1_1::class, RestJson1::class])
    fun `it generates field descriptors for simple structures`(subject: Class<ProtocolGenerator>) {
        val generator = subject.getDeclaredConstructor().newInstance()

        val testHarness = codegenTestHarnessForModelSnippet(generator, operations = listOf("Foo")) {
            """
            @http(method: "POST", uri: "/foo")
            operation Foo {
                input: FooRequest,
                output: FooRequest
            }  
            
            structure FooRequest { 
                strVal: String,
                intVal: Integer
            }
        """
        }

        val expectedDescriptors = """
            private val INTVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Integer, JsonSerialName("intVal"))
            private val STRVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("strVal"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                field(INTVAL_DESCRIPTOR)
                field(STRVAL_DESCRIPTOR)
            }
        """.formatForTest("        ")
        val actual = testHarness.generateDeSerializers()

        actual["FooOperationSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedDescriptors)
        actual["FooOperationDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedDescriptors)
    }

    @ParameterizedTest
    @ValueSource(classes = [AwsJson1_0::class, AwsJson1_1::class, RestJson1::class])
    fun `it generates nested field descriptors`(subject: Class<ProtocolGenerator>) {
        val generator = subject.getDeclaredConstructor().newInstance()

        val testHarness = codegenTestHarnessForModelSnippet(generator, operations = listOf("Foo")) {
            """            
            @http(method: "POST", uri: "/foo")
            operation Foo {
                input: FooRequest,
                output: FooRequest
            }  
            
            structure FooRequest { 
                payload: BarListList
            }
            
            list BarListList {
                member: BarList
            }
            
            list BarList {
                member: Bar
            }
            
            structure Bar {
                someVal: String
            } """
        }

        val expectedOperationDescriptors = """
            private val PAYLOAD_DESCRIPTOR = SdkFieldDescriptor(SerialKind.List, JsonSerialName("payload"))
            private val PAYLOAD_C0_DESCRIPTOR = SdkFieldDescriptor(SerialKind.List, JsonSerialName("payload_C0"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                field(PAYLOAD_DESCRIPTOR)
            }
        """.formatForTest("        ")

        val expectedDocumentDescriptors = """
            private val SOMEVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("someVal"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                field(SOMEVAL_DESCRIPTOR)
            }
        """.formatForTest("        ")

        val actual = testHarness.generateDeSerializers()

        actual["FooOperationSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedOperationDescriptors)
        actual["BarDocumentSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedDocumentDescriptors)
        actual["FooOperationDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedOperationDescriptors)
        actual["BarDocumentDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedDocumentDescriptors)
    }

    @ParameterizedTest
    @ValueSource(classes = [AwsJson1_0::class, AwsJson1_1::class, RestJson1::class])
    fun `it generates field descriptors for nested unions`(subject: Class<ProtocolGenerator>) {
        val generator = subject.getDeclaredConstructor().newInstance()

        val testHarness = codegenTestHarnessForModelSnippet(generator, operations = listOf("Foo")) {
            """
            @http(method: "POST", uri: "/foo")
            operation Foo {
                input: FooRequest,
                output: FooRequest
            }   
                 
            structure FooRequest { 
                payload: FooUnion
            }
            
            union FooUnion {
                structList: BarList
            }
            
            list BarList {
                member: BarStruct
            }
            
            structure BarStruct {
                someValue: FooUnion
            }
        """
        }

        val expectedDocumentDescriptors = """
            private val STRUCTLIST_DESCRIPTOR = SdkFieldDescriptor(SerialKind.List, JsonSerialName("structList"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                field(STRUCTLIST_DESCRIPTOR)
            }
        """.formatForTest("        ")

        val expectedOperationDescriptors = """
            private val PAYLOAD_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, JsonSerialName("payload"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                field(PAYLOAD_DESCRIPTOR)
            }
        """.formatForTest("        ")

        val expectedBarStructDescriptors = """
            private val SOMEVALUE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, JsonSerialName("someValue"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                field(SOMEVALUE_DESCRIPTOR)
            }
        """.formatForTest("        ")

        val actual = testHarness.generateDeSerializers()

        actual["FooUnionDocumentSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedDocumentDescriptors)
        actual["FooUnionDocumentDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedDocumentDescriptors)
        actual["FooOperationSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedOperationDescriptors)
        actual["FooOperationDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedOperationDescriptors)
        actual["BarStructDocumentSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedBarStructDescriptors)
        actual["BarStructDocumentDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedBarStructDescriptors)
    }

    @ParameterizedTest
    @ValueSource(classes = [AwsJson1_0::class, AwsJson1_1::class, RestJson1::class])
    fun `it generates expected import declarations`(subject: Class<ProtocolGenerator>) {
        val generator = subject.getDeclaredConstructor().newInstance()

        val testHarness = codegenTestHarnessForModelSnippet(generator, operations = listOf("Foo")) {
            """
            @http(method: "POST", uri: "/foo")
            operation Foo {
                input: FooRequest,
                output: FooRequest
            }   
                 
            structure FooRequest { 
                payload: String
            }            
        """
        }

        val expected = """
            import software.aws.clientrt.serde.*
            import software.aws.clientrt.serde.json.JsonSerialName
        """.formatForTest("")

        testHarness.generateDeSerializers().values.forEach { codegenFile ->
            codegenFile.shouldContainOnlyOnceWithDiff(expected)
        }
    }
}
