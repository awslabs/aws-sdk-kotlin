/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import io.kotest.extensions.system.withEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileCredentialsProviderTest {
    companion object {
        private const val DEFAULT_ID = "defaultId"
        private const val DEFAULT_KEY = "defaultKey"
        private val EXPECTED_DEFAULT_CREDS = Credentials(DEFAULT_ID, DEFAULT_KEY, null)

        private const val PROFILE_ID = "profileId"
        private const val PROFILE_KEY = "profileKey"
        private val EXPECTED_PROFILE_CREDS = Credentials(PROFILE_ID, PROFILE_KEY, null)

        private lateinit var credsPath: Path

        @BeforeAll
        @JvmStatic
        fun setup() {
            credsPath = Files.createTempFile("ProfileCredentialsProviderTest_creds", "")
            credsPath.toFile().writeText(
                """
                [default]
                aws_access_key_id = $DEFAULT_ID
                aws_secret_access_key = $DEFAULT_KEY
                
                [unique-profile-name]
                aws_access_key_id = $PROFILE_ID
                aws_secret_access_key = $PROFILE_KEY
                """.trimIndent()
            )
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            Files.deleteIfExists(credsPath)
        }
    }

    @Test
    fun `it should create a provider for a default profile`() = runBlocking {
        val provider = ProfileCredentialsProvider(credentialsFileName = credsPath.toString())
        val actual = provider.getCredentials()
        assertEquals(EXPECTED_DEFAULT_CREDS, actual)
    }

    @Test
    fun `it should create a provider for a unique profile specified in constructor`() = runBlocking {
        val provider = ProfileCredentialsProvider(profileName = "unique-profile-name", credentialsFileName = credsPath.toString())
        val actual = provider.getCredentials()
        assertEquals(EXPECTED_PROFILE_CREDS, actual)
    }

    @Test
    fun `it should create a provider for a unique profile specified from JVM Property`() = runBlocking {
        System.setProperty(AwsSdkSetting.AwsProfile.jvmProperty, "unique-profile-name")
        val provider = ProfileCredentialsProvider(credentialsFileName = credsPath.toString())
        val actual = provider.getCredentials()
        assertEquals(EXPECTED_PROFILE_CREDS, actual)
    }

    @Test
    fun `it should create a provider for a unique profile specified from Environment Variable`() = runBlocking {
        withEnvironment(AwsSdkSetting.AwsProfile.environmentVariable, "unique-profile-name") {
            val provider = ProfileCredentialsProvider(credentialsFileName = credsPath.toString())
            val actual = provider.getCredentials()
            assertEquals(EXPECTED_PROFILE_CREDS, actual)
        }
    }
}
