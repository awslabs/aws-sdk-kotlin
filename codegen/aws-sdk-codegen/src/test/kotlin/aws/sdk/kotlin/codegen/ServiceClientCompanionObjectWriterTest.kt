/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.test.TestModelDefault
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import kotlin.test.*

class ServiceClientCompanionObjectWriterTest {
    @Test
    fun testBase() {
        val writer = KotlinWriter(TestModelDefault.NAMESPACE)
        writer.putContext(
            mapOf(
                "ServiceSymbol" to buildSymbol { name = "TestGeneratorClient" },
                "SdkId" to "Test Generator",
            ),
        )
        ServiceClientCompanionObjectWriter().write(writer, null)

        val expected = """
            public companion object : AbstractAwsSdkClientFactory<Config, Config.Builder, TestGeneratorClient, Builder>() {
                @JvmStatic
                override fun builder(): Builder = Builder()

                override suspend fun finalizeConfig(builder: Builder, sharedConfig: LazyAsyncValue<AwsSharedConfig>, activeProfile: LazyAsyncValue<AwsProfile>) {
                    builder.config.endpointUrl = builder.config.endpointUrl ?: resolveEndpointUrl(
                        sharedConfig,
                        "TestGenerator",
                        "TEST_GENERATOR",
                        "test_generator",
                    )
                }
            
                override operator fun invoke(block: Config.Builder.() -> Unit): TestGeneratorClient = builder().apply {
                    config.apply(block)
                    config.interceptors.add(0, ClockSkewInterceptor())
                }.build()
            }
        """.trimIndent()

        writer.toString().shouldContainOnlyOnceWithDiff(expected)
    }
}
