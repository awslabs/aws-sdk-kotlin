/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.protocols.core

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.test.defaultSettings
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.TimestampFormatTrait
import kotlin.test.Test

class AwsHttpBindingProtocolGeneratorTest {

    @Test
    fun `it throws base service exception on error parse failure`() {
        val model = """
            namespace com.test
            use aws.protocols#restJson1

            @restJson1
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {
                errors: [FooError]
            }
            
            @error("server")
            structure FooError {
                payload: String
            }
        """.toSmithyModel()

        // This is the value that produces the name of the service base exception type
        val serviceSdkName = "SdkName"

        val testCtx = model.newTestContext(
            serviceName = "Example",
            settings = model.defaultSettings(sdkId = serviceSdkName)
        )
        val writer = KotlinWriter("com.test")
        val unit = TestableAwsHttpBindingProtocolGenerator()
        val op = model.expectShape<OperationShape>("com.test#GetFoo")

        unit.renderThrowOperationError(testCtx.generationCtx, op, writer)

        val actual = writer.toString()
        val expected = """
            throw ${serviceSdkName}Exception("Failed to parse response as 'restJson1' error", ex).also {
        """.trimIndent()

        actual.shouldContainOnlyOnceWithDiff(expected)
    }

    // A concrete implementation of AwsHttpBindingProtocolGenerator to exercise renderThrowOperationError()
    class TestableAwsHttpBindingProtocolGenerator : AwsHttpBindingProtocolGenerator() {
        override fun renderDeserializeErrorDetails(
            ctx: ProtocolGenerator.GenerationContext,
            op: OperationShape,
            writer: KotlinWriter
        ) {
            // NOP
        }

        override val defaultTimestampFormat: TimestampFormatTrait.Format
            get() = throw RuntimeException("Unneeded for test")

        override fun getProtocolHttpBindingResolver(model: Model, serviceShape: ServiceShape): HttpBindingResolver {
            throw RuntimeException("Unneeded for test")
        }

        override fun renderSerializeOperationBody(
            ctx: ProtocolGenerator.GenerationContext,
            op: OperationShape,
            writer: KotlinWriter
        ) {
            throw RuntimeException("Unneeded for test")
        }

        override fun renderDeserializeOperationBody(
            ctx: ProtocolGenerator.GenerationContext,
            op: OperationShape,
            writer: KotlinWriter
        ) {
            throw RuntimeException("Unneeded for test")
        }

        override fun renderSerializeDocumentBody(
            ctx: ProtocolGenerator.GenerationContext,
            shape: Shape,
            writer: KotlinWriter
        ) {
            throw RuntimeException("Unneeded for test")
        }

        override fun renderDeserializeDocumentBody(
            ctx: ProtocolGenerator.GenerationContext,
            shape: Shape,
            writer: KotlinWriter
        ) {
            throw RuntimeException("Unneeded for test")
        }

        override fun renderDeserializeException(
            ctx: ProtocolGenerator.GenerationContext,
            shape: Shape,
            writer: KotlinWriter
        ) {
            throw RuntimeException("Unneeded for test")
        }

        override val protocol: ShapeId
            get() = throw RuntimeException("Unneeded for test")
    }
}
