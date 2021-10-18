/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.Platform
import io.kotest.matchers.maps.shouldContainAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that exercise logic associated with the filesystem
 */
class AWSConfigLoaderFilesystemTest {

    @TempDir
    @JvmField
    val tempDir: Path? = null

    @Test
    fun itLoadsConfigFileFromFilesystem() = runSuspendTest {
        requireNotNull(tempDir)
        val configFile = tempDir.resolve("config")
        val credentialsFile = tempDir.resolve("credentials")

        configFile.writeText("[profile foo]\nname = value")

        val testPlatform = mockPlatform(
            pathSegment = Platform.filePathSeparator, // Use actual value from Platform in mock
            awsProfileEnv = "foo",
            homeEnv = "/home/user",
            awsConfigFileEnv = configFile.absolutePathString(),
            awsSharedCredentialsFileEnv = credentialsFile.absolutePathString(),
            os = Platform.osInfo() // Actual value
        )

        val actual = loadActiveAwsProfile(testPlatform)

        assertEquals("foo", actual.name)
        assertTrue(actual.containsKey("name"))
        assertEquals("value", actual["name"])

        configFile.deleteIfExists()
        credentialsFile.deleteIfExists()
    }

    @Test
    fun itLoadsConfigAndCredsFileFromFilesystem() = runSuspendTest {
        requireNotNull(tempDir)
        val configFile = tempDir.resolve("config")
        val credentialsFile = tempDir.resolve("credentials")

        configFile.writeText("[profile default]\nname = value\n[default]\nname2 = value2\n[profile default]\nname3 = value3")
        credentialsFile.writeText("[default]\nsecret=foo")

        val testPlatform = mockPlatform(
            pathSegment = Platform.filePathSeparator, // Use actual value from Platform in mock
            homeEnv = "/home/user",
            awsConfigFileEnv = configFile.absolutePathString(),
            awsSharedCredentialsFileEnv = credentialsFile.absolutePathString(),
            os = Platform.osInfo() // Actual value
        )

        val actual = loadActiveAwsProfile(testPlatform)

        assertEquals("default", actual.name)
        assertEquals(3, actual.size)
        actual.shouldContainAll(
            mapOf(
                "name" to "value", "name3" to "value3", "secret" to "foo"
            )
        )

        configFile.deleteIfExists()
        credentialsFile.deleteIfExists()
    }

    internal fun mockPlatform(
        pathSegment: String,
        awsProfileEnv: String? = null,
        awsConfigFileEnv: String? = null,
        homeEnv: String? = null,
        awsSharedCredentialsFileEnv: String? = null,
        homeProp: String? = null,
        os: OperatingSystem
    ): Platform {
        val testPlatform = mockk<Platform>()
        val envKeyParam = slot<String>()
        val propKeyParam = slot<String>()

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

        return testPlatform
    }
}
