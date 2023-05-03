/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.PlatformProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.assertEquals

/**
 * Tests that exercise logic associated with the filesystem
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AWSConfigLoaderFilesystemTest {

    @TempDir
    @JvmField
    var tempDir: Path? = null

    @Test
    fun itLoadsConfigFileFromFilesystem() = runTest {
        val configFile = tempDir!!.resolve("config")
        val credentialsFile = tempDir!!.resolve("credentials")

        configFile.writeText("[profile foo]\nname = value")

        val testPlatform = mockPlatform(
            pathSegment = PlatformProvider.System.filePathSeparator, // Use actual value from Platform in mock
            awsProfileEnv = "foo",
            homeEnv = "/home/user",
            awsConfigFileEnv = configFile.absolutePathString(),
            awsSharedCredentialsFileEnv = credentialsFile.absolutePathString(),
            os = PlatformProvider.System.osInfo(), // Actual value
        )

        val actual = loadAwsSharedConfig(testPlatform).activeProfile

        assertEquals("foo", actual.name)
        assertEquals("value", actual.getOrNull("name"))

        configFile.deleteIfExists()
        credentialsFile.deleteIfExists()
    }

    @Test
    fun itLoadsConfigAndCredsFileFromFilesystem() = runTest {
        val configFile = tempDir!!.resolve("config")
        val credentialsFile = tempDir!!.resolve("credentials")

        configFile.writeText("[profile default]\nname = value\n[default]\nname2 = value2\n[profile default]\nname3 = value3")
        credentialsFile.writeText("[default]\nsecret=foo")

        val testPlatform = mockPlatform(
            pathSegment = PlatformProvider.System.filePathSeparator, // Use actual value from Platform in mock
            homeEnv = "/home/user",
            awsConfigFileEnv = configFile.absolutePathString(),
            awsSharedCredentialsFileEnv = credentialsFile.absolutePathString(),
            os = PlatformProvider.System.osInfo(), // Actual value
        )

        val actual = loadAwsSharedConfig(testPlatform).activeProfile

        assertEquals("default", actual.name)
        assertEquals("value", actual.getOrNull("name"))
        assertEquals("value3", actual.getOrNull("name3"))
        assertEquals("foo", actual.getOrNull("secret"))

        configFile.deleteIfExists()
        credentialsFile.deleteIfExists()
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
        val filePath = slot<String>()

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
        coEvery {
            testPlatform.readFileOrNull(capture(filePath))
        } answers {
            if (awsConfigFileEnv != null) {
                val file = if (filePath.captured.endsWith("config")) {
                    File(awsConfigFileEnv)
                } else {
                    File(awsSharedCredentialsFileEnv)
                }

                if (file.exists()) file.readBytes() else null
            } else {
                null
            }
        }

        return testPlatform
    }
}
