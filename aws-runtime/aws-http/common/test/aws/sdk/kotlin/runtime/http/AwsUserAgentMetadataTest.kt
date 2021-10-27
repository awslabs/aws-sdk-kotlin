/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.smithy.kotlin.runtime.util.OsFamily
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsUserAgentMetadataTest {

    @Test
    fun testUserAgent() {
        val provider = TestPlatformProvider()
        val ua = loadAwsUserAgentMetadataFromEnvironment(provider, ApiMetadata("Test Service", "1.2.3"))
        assertEquals("aws-sdk-kotlin/1.2.3", ua.userAgent)
    }

    @Test
    fun testXAmzUserAgent() {
        val apiMeta = ApiMetadata("Test Service", "1.2.3")
        val sdkMeta = SdkMetadata("kotlin", apiMeta.version)
        val osMetadata = OsMetadata(OsFamily.Linux, "ubuntu-20.04")
        val langMeta = LanguageMetadata("1.4.31", mapOf("jvmVersion" to "1.11"))
        val ua = AwsUserAgentMetadata(sdkMeta, apiMeta, osMetadata, langMeta)
        val expected = "aws-sdk-kotlin/1.2.3 api/test-service/1.2.3 os/linux/ubuntu-20.04 lang/kotlin/1.4.31 md/jvmVersion/1.11"
        assertEquals(expected, ua.xAmzUserAgent)
    }

    data class EnvironmentTest(
        val provider: TestPlatformProvider,
        val expected: String
    )

    @Test
    fun testFrameworkFromEnvironment() {
        val testEnvironments = listOf(
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(FRAMEWORK_METADATA_ENV to "amplify:1.2.3")
                ),
                "lib/amplify/1.2.3"
            ),
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(FRAMEWORK_METADATA_ENV to "amplify:1.2.3"),
                    props = mapOf(FRAMEWORK_METADATA_PROP to "amplify:4.5.6")
                ),
                "lib/amplify/4.5.6"
            ),
        )
        testEnvironments.forEach { test ->
            val actual = FrameworkMetadata.fromEnvironment(test.provider).toString()
            assertEquals(test.expected, actual)
        }
    }

    @Test
    fun testAppIdFromEnvironment() {
        val testEnvironments = listOf(
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(AWS_APP_ID_ENV to "app-id-1")
                ),
                "app/app-id-1"
            ),
            EnvironmentTest(
                TestPlatformProvider(
                    env = mapOf(AWS_APP_ID_ENV to "app-id-1"),
                    props = mapOf(AWS_APP_ID_PROP to "app-id-2"),
                ),
                "app/app-id-2"
            ),
        )
        testEnvironments.forEach { test ->
            val actual = loadAwsUserAgentMetadataFromEnvironment(test.provider, ApiMetadata("Test Service", "1.2.3"))
            actual.xAmzUserAgent.shouldContain(test.expected)
        }
    }

    @Test
    fun testCustomMetadata() {
        val provider = TestPlatformProvider()
        val metadata = loadAwsUserAgentMetadataFromEnvironment(provider, ApiMetadata("Test Service", "1.2.3"))
        val customMetadata = CustomUserAgentMetadata()

        customMetadata.add("foo", "bar")
        customMetadata.add("truthy", "true")
        customMetadata.add("falsey", "false")

        val configMetadata = ConfigMetadata("retry-mode", "standard")
        customMetadata.add(configMetadata)

        customMetadata.add(FeatureMetadata("s3-transfer", "1.2.3"))
        customMetadata.add(FeatureMetadata("waiter"))

        val actual = metadata.copy(customMetadata = customMetadata).xAmzUserAgent

        listOf(
            "md/foo/bar",
            "md/truthy",
            "md/falsey/false",
            "cfg/retry-mode/standard",
            "ft/s3-transfer/1.2.3",
            "ft/waiter"
        ).forEach { partial ->
            actual.shouldContain(partial)
        }
    }
}
