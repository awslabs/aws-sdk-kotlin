/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.xml

import software.amazon.smithy.kotlin.codegen.test.*
import software.amazon.smithy.model.shapes.ShapeId
import kotlin.test.Test

class RestXmlSerdeDescriptorGeneratorTest {
    private fun render(modelSnippet: String): String {
        val model = modelSnippet.prependNamespaceAndService().toSmithyModel()

        val testCtx = model.newTestContext()
        val writer = testCtx.newWriter()
        val shape = model.expectShape(ShapeId.from("com.test#Foo"))
        val renderingCtx = testCtx.toRenderingContext(writer, shape)

        RestXmlSerdeDescriptorGenerator(renderingCtx).render()
        return writer.toString()
    }

    @Test
    fun `it should add alias for message field in error struct`() {
        val generated = render(
            """
                @error("client")
                structure Foo { 
                    message: String,
                    foo: String
                }
            """.trimIndent(),
        )

        val expectedDescriptors = """
            val FOO_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("foo"))
            val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("message"), XmlAliasName("Message"))
        """.formatForTest("")

        generated.shouldContainOnlyOnceWithDiff(expectedDescriptors)
    }

    @Test
    fun `it should not add alias for message field in non-error struct`() {
        val generated = render(
            """
                structure Foo { 
                    message: String
                }
            """.trimIndent(),
        )

        val expectedDescriptors = """
            val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("message"))
        """.formatForTest("")

        generated.shouldContainOnlyOnceWithDiff(expectedDescriptors)
    }
}
