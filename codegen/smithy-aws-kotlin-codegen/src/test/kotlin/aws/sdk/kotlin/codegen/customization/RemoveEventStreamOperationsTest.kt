/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization

import io.kotest.matchers.shouldBe
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.prependNamespaceAndService
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.shapes.ShapeId
import kotlin.test.Test

class RemoveEventStreamOperationsTest {
    @Test
    fun testEventStreamOperationsAreRemovedFromTheModel() {
        val model = """
        operation EventStream {
            input: StreamingInput,
        }
        operation BlobStream{
            input: BlobInput
        }
        structure BlobInput {
            @required
            blob: StreamingBlob
        }
        @streaming
        blob StreamingBlob
        structure StreamingInput {
            payload: Event
        }
        @streaming
        union Event {
            s: Foo
        }
        structure Foo {}
        """.prependNamespaceAndService(operations = listOf("EventStream", "BlobStream")).toSmithyModel()

        val ctx = model.newTestContext()
        val transformed = RemoveEventStreamOperations().preprocessModel(model, ctx.generationCtx.settings)
        transformed.expectShape(ShapeId.from("com.test#BlobStream"))
        transformed.getShape(ShapeId.from("com.test#EventStream")).shouldBe(java.util.Optional.empty())
    }
}
