/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.config.utils.mockPlatform
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.assertEquals

/**
 * Tests that exercise logic associated with the filesystem
 */
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
}
