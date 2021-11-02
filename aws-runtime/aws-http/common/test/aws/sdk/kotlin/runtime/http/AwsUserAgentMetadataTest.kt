/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.http.operation.CustomUserAgentMetadata
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
        val custom = CustomUserAgentMetadata().apply {
            add("foo", "bar")
        }
        val ua = AwsUserAgentMetadata(sdkMeta, apiMeta, osMetadata, langMeta, customMetadata = custom)
        val expected = "aws-sdk-kotlin/1.2.3 api/test-service/1.2.3 os/linux/ubuntu-20.04 lang/kotlin/1.4.31 md/jvmVersion/1.11 md/foo/bar"
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
}
