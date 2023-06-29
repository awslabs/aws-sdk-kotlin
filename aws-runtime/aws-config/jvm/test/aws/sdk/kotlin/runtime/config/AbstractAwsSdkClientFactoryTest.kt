/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.client.AwsSdkClientConfig
import aws.smithy.kotlin.runtime.client.*
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
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
                AwsSdkSetting.AwsRegion.sysProp to "resolved-region",
                AwsSdkSetting.AwsRetryMode.sysProp to "standard",
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

    class Config private constructor(builder: Builder) : SdkClientConfig, AwsSdkClientConfig, RetryStrategyClientConfig by builder.buildRetryStrategyClientConfig() {
        override val clientName: String = builder.clientName
        override val logMode: LogMode = builder.logMode ?: LogMode.Default
        override val region: String = builder.region ?: error("region is required")
        override var useFips: Boolean = builder.useFips ?: false
        override var useDualStack: Boolean = builder.useDualStack ?: false

        // new: inherits builder equivalents for Config base classes
        class Builder : AwsSdkClientConfig.Builder, SdkClientConfig.Builder<Config>, RetryStrategyClientConfig.Builder by RetryStrategyClientConfigImpl.BuilderImpl() {
            override var clientName: String = "Test"
            override var logMode: LogMode? = LogMode.Default
            override var region: String? = null
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
