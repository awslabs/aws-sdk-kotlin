/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.xml

import aws.sdk.kotlin.codegen.protocols.RestXml
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.test.codegenTestHarnessForModelSnippet
import software.amazon.smithy.kotlin.codegen.test.formatForTest
import software.amazon.smithy.kotlin.codegen.test.generateDeSerializers
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff

class RestXmlFieldObjectDescriptorTest {

    @ParameterizedTest
    @ValueSource(classes = [RestXml::class])
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
            private val INTVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Integer, XmlSerialName("intVal"))
            private val STRVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("strVal"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                trait(XmlSerialName("FooRequest"))
                field(INTVAL_DESCRIPTOR)
                field(STRVAL_DESCRIPTOR)
            }
        """.formatForTest("        ")
        val actual = testHarness.generateDeSerializers()

        actual["FooOperationSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedDescriptors)
        actual["FooOperationDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedDescriptors)
    }

    @ParameterizedTest
    @ValueSource(classes = [RestXml::class])
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
            private val PAYLOAD_DESCRIPTOR = SdkFieldDescriptor(SerialKind.List, XmlSerialName("payload"))
            private val PAYLOAD_C0_DESCRIPTOR = SdkFieldDescriptor(SerialKind.List, XmlSerialName("member"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                trait(XmlSerialName("FooRequest"))
                field(PAYLOAD_DESCRIPTOR)
            }
        """.formatForTest("        ")

        val expectedDocumentDescriptors = """
            private val SOMEVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("someVal"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                trait(XmlSerialName("Bar"))
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
    @ValueSource(classes = [RestXml::class])
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
            private val STRUCTLIST_DESCRIPTOR = SdkFieldDescriptor(SerialKind.List, XmlSerialName("structList"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                trait(XmlSerialName("FooUnion"))
                field(STRUCTLIST_DESCRIPTOR)
            }
        """.formatForTest("        ")

        val expectedOperationDescriptors = """
            private val PAYLOAD_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("payload"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                trait(XmlSerialName("FooRequest"))
                field(PAYLOAD_DESCRIPTOR)
            }
        """.formatForTest("        ")

        val expectedBarStructDescriptors = """
            private val SOMEVALUE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("someValue"))
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                trait(XmlSerialName("BarStruct"))
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
    @ValueSource(classes = [RestXml::class])
    fun `it generates expected import declarations`(subject: Class<ProtocolGenerator>) {
        val generator = subject.getDeclaredConstructor().newInstance()

        val testHarness = codegenTestHarnessForModelSnippet(generator, operations = listOf("Foo")) {
            """
            @http(method: "POST", uri: "/foo")
            operation Foo {
                input: FooRequest,
                output: FooRequest
            }   
                 
            @xmlName("CustomFooRequest")
            structure FooRequest {
                @xmlAttribute
                payload: String,
                @xmlFlattened
                listVal: ListOfString
            }
                        
            list ListOfString {
                member: String
            }
        """
        }

        val expected = """
            import software.aws.clientrt.serde.*
            import software.aws.clientrt.serde.xml.Flattened
            import software.aws.clientrt.serde.xml.XmlAttribute
            import software.aws.clientrt.serde.xml.XmlSerialName
        """.formatForTest("")

        testHarness.generateDeSerializers().values.forEach { codegenFile ->
            codegenFile.shouldContainOnlyOnceWithDiff(expected)
        }
    }

    @ParameterizedTest
    @ValueSource(classes = [RestXml::class])
    fun `it generates field descriptors for flattened xml trait and object descriptor for XmlName trait`(subject: Class<ProtocolGenerator>) {
        val generator = subject.getDeclaredConstructor().newInstance()

        val testHarness = codegenTestHarnessForModelSnippet(generator, operations = listOf("Foo")) {
            """
            @http(method: "POST", uri: "/foo")
            operation Foo {
                input: FooRequest,
                output: FooRequest
            }  
            
            @xmlName("CustomFooRequest")
            structure FooRequest {
                @xmlFlattened
                listVal: ListOfString,
                @xmlFlattened
                mapVal: MapOfInteger
            }
            
            list ListOfString {
                member: String
            }
            
            map MapOfInteger {
                key: String,
                value: String
            }
        """
        }

        val expectedDescriptors = """
            private val LISTVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.List, XmlSerialName("listVal"), Flattened)
            private val MAPVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Map, XmlSerialName("mapVal"), Flattened)
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                trait(XmlSerialName("CustomFooRequest"))
                field(LISTVAL_DESCRIPTOR)
                field(MAPVAL_DESCRIPTOR)
            }
        """.formatForTest("        ")
        val actual = testHarness.generateDeSerializers()

        actual["FooOperationSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedDescriptors)
        actual["FooOperationDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedDescriptors)
    }

    @ParameterizedTest
    @ValueSource(classes = [RestXml::class])
    fun `it generates field descriptors for xml attributes and namespace`(subject: Class<ProtocolGenerator>) {
        val generator = subject.getDeclaredConstructor().newInstance()

        val testHarness = codegenTestHarnessForModelSnippet(generator, operations = listOf("Foo")) {
            """
            @http(method: "POST", uri: "/foo")
            operation Foo {
                input: FooRequest,
                output: FooRequest
            }  
            
            @xmlNamespace(uri: "http://foo.com", prefix: "baz")
            structure FooRequest {
                @xmlAttribute
                strVal: String,
                @xmlAttribute
                @xmlName("baz:notIntVal")
                intVal: Integer
            }
        """
        }

        val expectedDescriptors = """
            private val INTVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Integer, XmlSerialName("baz:notIntVal"), XmlAttribute)
            private val STRVAL_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("strVal"), XmlAttribute)
            private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                trait(XmlSerialName("FooRequest"))
                trait(XmlNamespace("http://foo.com", "baz"))
                field(INTVAL_DESCRIPTOR)
                field(STRVAL_DESCRIPTOR)
            }
        """.formatForTest("        ")
        val actual = testHarness.generateDeSerializers()

        actual["FooOperationSerializer.kt"].shouldContainOnlyOnceWithDiff(expectedDescriptors)
        actual["FooOperationDeserializer.kt"].shouldContainOnlyOnceWithDiff(expectedDescriptors)
    }
}
