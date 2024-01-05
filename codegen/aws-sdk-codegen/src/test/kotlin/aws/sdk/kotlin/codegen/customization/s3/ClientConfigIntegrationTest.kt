/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.testutil.model
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientConfigGenerator
import software.amazon.smithy.kotlin.codegen.test.TestModelDefault
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import software.amazon.smithy.kotlin.codegen.test.toRenderingContext
import kotlin.test.Test

class ClientConfigIntegrationTest {
    @Test
    fun testRenderAuthSchemeProperty() {
        val model = model("S3")
        val ctx = model.newTestContext("S3")
        val writer = KotlinWriter(TestModelDefault.NAMESPACE)
        val serviceShape = model.serviceShapes.single()
        val renderingCtx = ctx
            .toRenderingContext(writer, serviceShape)
            .copy(integrations = listOf(ClientConfigIntegration()))

        val generator = ServiceClientConfigGenerator(serviceShape, detectDefaultProps = false)
        generator.render(renderingCtx, writer)
        val contents = writer.toString()

        val expectedImmutableProp = """
            override val authSchemes: kotlin.collections.List<aws.smithy.kotlin.runtime.http.auth.AuthScheme> = builder.authSchemes
        """.trimIndent()
        contents.shouldContainOnlyOnceWithDiff(expectedImmutableProp)

        val expectedBuilderProp = """
            /**
             * Register new or override default [AuthScheme]s configured for this client. By default, the set
             * of auth schemes configured comes from the service model. An auth scheme configured explicitly takes
             * precedence over the defaults and can be used to customize identity resolution and signing for specific
             * authentication schemes.
             */
            override var authSchemes: kotlin.collections.List<aws.smithy.kotlin.runtime.http.auth.AuthScheme> = listOf(aws.smithy.kotlin.runtime.http.auth.SigV4AsymmetricAuthScheme(aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner))
        """.replaceIndent("        ")
        contents.shouldContainOnlyOnceWithDiff(expectedBuilderProp)
    }
}
