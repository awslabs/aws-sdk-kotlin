package aws.sdk.kotlin.codegen.protocols.core

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.test.TestModelDefault
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import software.amazon.smithy.model.node.Node
import kotlin.test.Test

class ExtraMetadataGeneratorTest {
    @Test
    fun testRender() {
        val writer = KotlinWriter(TestModelDefault.NAMESPACE)
        val metadataJson = """{ "foo": "bar", "baz": "qux" }"""
        val metadataNode = Node.parse(metadataJson).expectObjectNode()

        ExtraMetadataGenerator(writer, metadataNode).render()

        val actual = writer.toString()
        val expected = """
            internal val extraMetadata: Map<String, String> = mapOf(
                "foo" to "bar",
                "baz" to "qux",
            )
        """.trimIndent()
        actual.shouldContainOnlyOnceWithDiff(expected)
    }
}
