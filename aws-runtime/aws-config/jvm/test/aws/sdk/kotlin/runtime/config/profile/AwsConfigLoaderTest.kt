/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AwsConfigLoaderTest {

    @Test
    fun canPassTestSuite() {
        val testCases = Json.parseToJsonElement(LOADER_TEST_SUITE_JSON).jsonObject["tests"]!!.jsonArray

        testCases
            .map { TestCase.fromJson(it.jsonObject) }
            // .filter { testCase -> testCase.name == "The default config location can be overridden by the user on non-windows platforms." }
            .forEach { testCase ->
                val testPlatform = mockPlatform(testCase)

                val actual = resolveConfigSource(testPlatform)

                if (testCase.profile != null) {
                    assertEquals(testCase.profile, actual.profile)
                } else {
                    assertTrue(actual.profile == "default") // test cases that do not define profiles should use default
                }
                assertEquals(testCase.profile, if (testCase.profile != null) actual.profile else null)
                assertEquals(testCase.configLocation, actual.configPath)
                assertEquals(testCase.credentialsLocation, actual.credentialsPath)
            }
    }

    @Test
    fun itLoadsAWSConfigurationWithCustomProfile() = runTest {
        val testPlatform = mockPlatform(
            pathSegment = "/",
            awsProfileEnv = "bob",
            homeEnv = "/home/user",
            os = OperatingSystem(OsFamily.Linux, null),
        )

        val config = loadAwsSharedConfig(testPlatform)
        val activeProfile = config.activeProfile
        assertEquals("bob", activeProfile.name)
    }

    @Test
    fun configurationLoadingDoesNotThrowErrors() = runTest {
        val config = loadAwsSharedConfig(PlatformProvider.System)
        val activeProfile = config.activeProfile

        assertTrue(activeProfile.name.isNotBlank())
    }

    private fun mockPlatform(
        pathSegment: String,
        awsProfileEnv: String? = null,
        awsConfigFileEnv: String? = null,
        homeEnv: String? = null,
        awsSharedCredentialsFileEnv: String? = null,
        homeProp: String? = null,
        os: OperatingSystem,
    ): PlatformProvider {
        val testPlatform = mockk<PlatformProvider>()
        val envKeyParam = slot<String>()
        val propKeyParam = slot<String>()
        val readFileParam = slot<String>()

        every { testPlatform.filePathSeparator } returns pathSegment
        every { testPlatform.getenv(capture(envKeyParam)) } answers {
            when (envKeyParam.captured) {
                "AWS_PROFILE" -> awsProfileEnv
                "AWS_CONFIG_FILE" -> awsConfigFileEnv
                "HOME" -> homeEnv
                "AWS_SHARED_CREDENTIALS_FILE" -> awsSharedCredentialsFileEnv
                else -> error(envKeyParam.captured)
            }
        }
        every { testPlatform.getProperty(capture(propKeyParam)) } answers {
            if (propKeyParam.captured == "user.home") homeProp else null
        }
        every { testPlatform.osInfo() } returns os

        coEvery { testPlatform.readFileOrNull(capture(readFileParam)) } answers { null }

        return testPlatform
    }

    private fun mockPlatform(testCase: TestCase): PlatformProvider {
        val testPlatform = mockk<PlatformProvider>()
        val envKeyParam = slot<String>()
        val propKeyParam = slot<String>()

        every { testPlatform.filePathSeparator } returns when (testCase.platform) {
            OsFamily.Windows -> "\\"
            else -> "/"
        }
        every { testPlatform.getenv(capture(envKeyParam)) } answers {
            (testCase.environment[envKeyParam.captured] as JsonPrimitive?)?.content
        }
        every { testPlatform.getProperty(capture(propKeyParam)) } answers {
            if (propKeyParam.captured == "user.home") testCase.languageSpecificHome else null
        }
        every { testPlatform.osInfo() } returns OperatingSystem(testCase.platform, null)

        return testPlatform
    }

    private data class TestCase(
        val name: String,
        val environment: Map<String, JsonElement>,
        val languageSpecificHome: String?,
        val platform: OsFamily,
        val profile: String?,
        val configLocation: String,
        val credentialsLocation: String,
    ) {
        companion object {
            fun fromJson(json: JsonObject): TestCase {
                val name = (json["name"] as JsonPrimitive).content
                val environment: Map<String, JsonElement> = json["environment"] as JsonObject
                val languageSpecificHome = (json["languageSpecificHome"] as JsonPrimitive?)?.content
                val platformRaw = (json["platform"] as JsonPrimitive).content
                val profile = (json["profile"] as JsonPrimitive?)?.content
                val configLocation = (json["configLocation"] as JsonPrimitive).content
                val credentialsLocation = (json["credentialsLocation"] as JsonPrimitive).content

                val platform = when (platformRaw) {
                    "windows" -> OsFamily.Windows
                    "linux" -> OsFamily.Linux
                    else -> error("Unexpected platform $platformRaw")
                }

                return TestCase(
                    name,
                    environment,
                    languageSpecificHome,
                    platform,
                    profile,
                    configLocation,
                    credentialsLocation,
                )
            }
        }
    }
}
