/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.core
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpTraitResolver
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import kotlin.test.Test

class AwsHttpProtocolClientGeneratorTest {
    @Test
    fun testRetryPolicyIsOverridden() {
        val model = """
            namespace com.test

            use aws.protocols#awsJson1_1
            use aws.api#service

            @service(sdkId: "Test", endpointPrefix: "test.service")
            @awsJson1_1
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            @http(method: "GET", uri: "/foo")
            operation GetFoo {}
        """.toSmithyModel()

        val testCtx = model.newTestContext(serviceName = "Example")
        val writer = KotlinWriter("com.test")

        val generator = AwsHttpProtocolClientGenerator(
            ctx = testCtx.generationCtx,
            middlewares = emptyList(),
            httpBindingResolver = HttpTraitResolver(testCtx.generationCtx, "application/json"),
        )

        generator.render(writer)
        val contents = writer.toString()
        val expected = """
        op.execution.retryStrategy = config.retryStrategy
        op.execution.retryPolicy = AwsDefaultRetryPolicy
        """
        contents.shouldContainOnlyOnceWithDiff(expected)
    }
}
