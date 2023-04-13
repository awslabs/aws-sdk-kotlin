/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.client.AwsSdkClientConfig
import aws.sdk.kotlin.runtime.http.retries.AwsDefaultRetryPolicy
import aws.smithy.kotlin.runtime.client.AbstractSdkClientBuilder
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.client.SdkClientConfig
import aws.smithy.kotlin.runtime.client.SdkLogMode
import aws.smithy.kotlin.runtime.retries.Outcome
import aws.smithy.kotlin.runtime.retries.RetryOptions
import aws.smithy.kotlin.runtime.retries.RetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.policy.RetryPolicy
import io.kotest.extensions.system.withSystemProperties
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AbstractAwsSdkClientFactoryTest {
    @Test
    fun testFromEnvironmentFavorsExplicitConfig() = runTest {
        val explicitRegion = "explicit-region"
        val explicitRetryStrategy = StandardRetryStrategy()
        val client = TestClient.fromEnvironment {
            region = explicitRegion
            retryStrategy = explicitRetryStrategy
        }

        assertEquals(explicitRegion, client.config.region)
        assertEquals(explicitRetryStrategy, client.config.retryStrategy)
    }

    @Test
    fun testFromEnvironmentResolvesDefaults() = runTest {
        withSystemProperties(
            mapOf(
                AwsSdkSetting.AwsRegion.jvmProperty to "resolved-region",
                AwsSdkSetting.AwsRetryMode.jvmProperty to "standard",
            ),
        ) {
            val client = TestClient.fromEnvironment()
            assertEquals("resolved-region", client.config.region)
            assertIs<StandardRetryStrategy>(client.config.retryStrategy)
        }
    }
}

private interface TestClient : SdkClient {
    override val config: Config

    // refactored: mostly in an abstract base now
    companion object : AbstractAwsSdkClientFactory<Config, Config.Builder, TestClient, Builder>() {
        override fun builder(): Builder = Builder()
    }

    class Builder internal constructor() : AbstractSdkClientBuilder<Config, Config.Builder, TestClient>() {
        override val config: Config.Builder = Config.Builder()
        override fun newClient(config: Config): TestClient = DefaultTestClient(config)
    }

    class Config private constructor(builder: Config.Builder) : SdkClientConfig, AwsSdkClientConfig {
        override val clientName: String = builder.clientName
        override val retryPolicy: RetryPolicy<Any?> = builder.retryPolicy ?: AwsDefaultRetryPolicy
        override val retryStrategy: RetryStrategy = builder.retryStrategy ?: TestRetryStrategy()
        override val region: String = builder.region ?: error("region is required")
        override var useFips: Boolean = builder.useFips ?: false
        override var useDualStack: Boolean = builder.useDualStack ?: false

        // new: inherits builder equivalents for Config base classes
        class Builder : AwsSdkClientConfig.Builder, SdkClientConfig.Builder<Config> {
            override var clientName: String = "Test"
            override var region: String? = null
            override var retryPolicy: RetryPolicy<Any?>? = null
            override var retryStrategy: RetryStrategy? = null
            override var sdkLogMode: SdkLogMode? = null
            override var useFips: Boolean? = null
            override var useDualStack: Boolean? = null
            override fun build(): Config = Config(this)
        }
    }
}

private class DefaultTestClient(
    override val config: TestClient.Config,
) : TestClient {
    override fun close() { }
}

// some non standard retry strategy used as a marker
private class TestRetryStrategy : RetryStrategy {
    override val options: RetryOptions
        get() = error("not needed for test")

    override suspend fun <R> retry(policy: RetryPolicy<R>, block: suspend () -> R): Outcome<R> {
        error("not needed for test")
    }
}
