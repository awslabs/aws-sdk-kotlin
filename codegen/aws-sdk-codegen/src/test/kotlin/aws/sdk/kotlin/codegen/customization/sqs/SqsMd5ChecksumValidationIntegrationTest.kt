/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.sqs

import aws.sdk.kotlin.codegen.testutil.model
import org.junit.jupiter.api.Test
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.test.defaultSettings
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.model.shapes.OperationShape
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class SqsMd5ChecksumValidationIntegrationTest {
    @Test
    fun testNotExpectedForNonSqsModel() {
        val model = model("NotSqs")
        val actual = SqsMd5ChecksumValidationIntegration().enabledForService(model, model.defaultSettings())

        assertFalse(actual)
    }

    @Test
    fun testExpectedForSqsModel() {
        val model = model("Sqs")
        val actual = SqsMd5ChecksumValidationIntegration().enabledForService(model, model.defaultSettings())

        assertTrue(actual)
    }

    @Test
    fun testMiddlewareAddition() {
        val model = model("Sqs")
        val preexistingMiddleware = listOf(FooMiddleware)
        val ctx = model.newTestContext("Sqs")
        val actual = SqsMd5ChecksumValidationIntegration().customizeMiddleware(ctx.generationCtx, preexistingMiddleware)

        assertEquals(listOf(FooMiddleware, SqsMd5ChecksumValidationMiddleware), actual)
    }
}

object FooMiddleware : ProtocolMiddleware {
    override val name: String = "FooMiddleware"
    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) =
        fail("Unexpected call to `FooMiddleware.render`")
}
