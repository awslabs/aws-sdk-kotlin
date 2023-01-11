/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.net.Host
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileCredentialsProviderTest {
    @Test
    fun testDefaultProfile() = runTest {
        val testProvider = TestPlatformProvider(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to """
                [default]
                aws_access_key_id = AKID-Default
                aws_secret_access_key = Default-Secret
                """.trimIndent(),
            ),
        )
        val testEngine = TestConnection()

        val provider = ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClientEngine = testEngine,
        )
        val actual = provider.getCredentials()
        val expected = Credentials("AKID-Default", "Default-Secret")
        assertEquals(expected, actual)
    }

    @Test
    fun testExplicitProfileOverride() = runTest {
        val testProvider = TestPlatformProvider(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to """
                [default]
                aws_access_key_id = AKID-Default
                aws_secret_access_key = Default-Secret
                
                [profile my-profile]
                aws_access_key_id = AKID-Profile
                aws_secret_access_key = Profile-Secret
                """.trimIndent(),
            ),
        )
        val testEngine = TestConnection()

        val provider = ProfileCredentialsProvider(
            profileName = "my-profile",
            platformProvider = testProvider,
            httpClientEngine = testEngine,
        )
        val actual = provider.getCredentials()
        val expected = Credentials("AKID-Profile", "Profile-Secret")
        assertEquals(expected, actual)
    }

    @Test
    fun testProfileOverrideFromEnvironment() = runTest {
        val testProvider = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                "AWS_PROFILE" to "my-profile",
            ),
            fs = mapOf(
                "config" to """
                [default]
                aws_access_key_id = AKID-Default
                aws_secret_access_key = Default-Secret
                
                [profile my-profile]
                aws_access_key_id = AKID-Profile
                aws_secret_access_key = Profile-Secret
                """.trimIndent(),
            ),
        )
        val testEngine = TestConnection()

        val provider = ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClientEngine = testEngine,
        )
        val actual = provider.getCredentials()
        val expected = Credentials("AKID-Profile", "Profile-Secret")
        assertEquals(expected, actual)
    }

    @Test
    fun testBasicAssumeRole() = runTest {
        // smoke test for assume role, more involved scenarios are tested through the default chain

        val testArn = "arn:aws:iam:1234567/test-role"
        val testProvider = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                "AWS_REGION" to "us-west-2",
            ),
            fs = mapOf(
                "config" to """
                [default]
                role_arn = $testArn
                source_profile = B
                
                [profile B]
                region = us-east-1
                aws_access_key_id = AKID-Profile
                aws_secret_access_key = Profile-Secret
                """.trimIndent(),
            ),
        )
        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse(testArn))
        }

        val provider = ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClientEngine = testEngine,
        )
        val actual = provider.getCredentials()
        assertEquals(StsTestUtils.expectedCredentialsBase, actual)

        testEngine.assertRequests()
        val req = testEngine.requests().first()
        // region is overridden from the environment which should take precedence
        assertEquals(Host.Domain("sts.us-west-2.amazonaws.com"), req.actual.url.host)
    }
}
