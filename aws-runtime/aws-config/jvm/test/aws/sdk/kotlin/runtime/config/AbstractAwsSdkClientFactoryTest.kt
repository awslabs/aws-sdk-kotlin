/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.client.AwsSdkClientConfig
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.config.useragent.resolveUserAgent
import aws.sdk.kotlin.runtime.config.utils.mockPlatform
import aws.smithy.kotlin.runtime.client.*
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy
import io.kotest.extensions.system.withEnvironment
import io.kotest.extensions.system.withSystemProperties
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AbstractAwsSdkClientFactoryTest {
    @JvmField
    @TempDir
    var tempDir: Path? = null

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

    @Test
    fun testFromEnvironmentResolvesAppId() = runTest {
        val credentialsFile = tempDir!!.resolve("credentials")
        val configFile = tempDir!!.resolve("config")

        configFile.writeText("[profile foo]\nsdk_ua_app_id = profile-app-id")

        val testPlatform = mockPlatform(
            pathSegment = PlatformProvider.System.filePathSeparator,
            awsProfileEnv = "foo",
            homeEnv = "/home/user",
            awsConfigFileEnv = configFile.absolutePathString(),
            awsSharedCredentialsFileEnv = credentialsFile.absolutePathString(),
            os = PlatformProvider.System.osInfo(),
        )

        val sharedConfig = asyncLazy { loadAwsSharedConfig(testPlatform) }
        val profile = asyncLazy { sharedConfig.get().activeProfile }

        assertEquals("profile-app-id", resolveUserAgent(testPlatform, profile))

        configFile.deleteIfExists()
        credentialsFile.deleteIfExists()

        withEnvironment(
            mapOf(
                AwsSdkSetting.AwsAppId.envVar to "env-app-id",
            ),
        ) {
            assertEquals("env-app-id", TestClient.fromEnvironment().config.sdkUserAgentAppId)

            withSystemProperties(
                mapOf(
                    AwsSdkSetting.AwsAppId.sysProp to "system-properties-app-id",
                ),
            ) {
                assertEquals("system-properties-app-id", TestClient.fromEnvironment().config.sdkUserAgentAppId)
                assertEquals(
                    "explicit-app-id",
                    TestClient.fromEnvironment { sdkUserAgentAppId = "explicit-app-id" }.config.sdkUserAgentAppId,
                )
            }
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
        override val sdkUserAgentAppId: String? = builder.sdkUserAgentAppId

        // new: inherits builder equivalents for Config base classes
        class Builder : AwsSdkClientConfig.Builder, SdkClientConfig.Builder<Config>, RetryStrategyClientConfig.Builder by RetryStrategyClientConfigImpl.BuilderImpl() {
            override var clientName: String = "Test"
            override var logMode: LogMode? = LogMode.Default
            override var region: String? = null
            override var useFips: Boolean? = null
            override var useDualStack: Boolean? = null
            override var sdkUserAgentAppId: String? = null
            override fun build(): Config = Config(this)
        }
    }
}

private class DefaultTestClient(
    override val config: TestClient.Config,
) : TestClient {
    override fun close() { }
}
