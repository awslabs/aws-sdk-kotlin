/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import aws.smithy.kotlin.runtime.util.attributesOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
            httpClient = testEngine,
        )
        val actual = provider.resolve()
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
            httpClient = testEngine,
        )
        val actual = provider.resolve()
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
            httpClient = testEngine,
        )
        val actual = provider.resolve()
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
            httpClient = testEngine,
        )
        val actual = provider.resolve()
        assertEquals(StsTestUtils.CREDENTIALS, actual)

        testEngine.assertRequests()
        val req = testEngine.requests().first()
        // region is overridden from the environment which should take precedence
        assertEquals(Host.Domain("sts.us-west-2.amazonaws.com"), req.actual.url.host)
    }

    @Test
    fun testExplicitRegion() = runTest {
        val testArn = "arn:aws:iam:1234567/test-role"
        val testProvider = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                "AWS_REGION" to "eu-west-3",
            ),
            fs = mapOf(
                "config" to """
                [default]
                role_arn = $testArn
                source_profile = B

                [profile B]
                aws_access_key_id = AKID-Profile
                aws_secret_access_key = Profile-Secret
                region = af-south-1 
                """.trimIndent(),
            ),
        )

        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse(testArn))
        }

        ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClient = testEngine,
            region = "us-west-2",
        ).resolve(
            attributesOf {
                AwsClientOption.Region to "cn-north-1"
            },
        )

        testEngine.assertRequests()
        val requests = testEngine.requests().first()
        assertEquals(Host.Domain("sts.us-west-2.amazonaws.com"), requests.actual.url.host)
    }

    @Test
    fun testProfileRegion() = runTest {
        val testArn = "arn:aws:iam:1234567/test-role"
        val testProvider = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                "AWS_REGION" to "eu-west-3",
            ),
            fs = mapOf(
                "config" to """
                [default]
                role_arn = $testArn
                region = us-west-2
                source_profile = B

                [profile B]
                aws_access_key_id = AKID-Profile
                aws_secret_access_key = Profile-Secret
                """.trimIndent(),
            ),
        )

        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse(testArn))
        }

        ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClient = testEngine,
            profileName = "default",
        ).resolve(
            attributesOf {
                AwsClientOption.Region to "cn-north-1"
            },
        )

        testEngine.assertRequests()
        val requests = testEngine.requests().first()
        assertEquals(Host.Domain("sts.us-west-2.amazonaws.com"), requests.actual.url.host)
    }

    @Test
    fun testAttributeRegion() = runTest {
        val testArn = "arn:aws:iam:1234567/test-role"
        val testProvider = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                "AWS_REGION" to "eu-west-3",
            ),
            fs = mapOf(
                "config" to """
                [default]
                role_arn = $testArn
                source_profile = B

                [profile B]
                aws_access_key_id = AKID-Profile
                aws_secret_access_key = Profile-Secret
                """.trimIndent(),
            ),
        )

        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse(testArn))
        }

        ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClient = testEngine,
        ).resolve(
            attributesOf {
                AwsClientOption.Region to "us-west-2"
            },
        )

        testEngine.assertRequests()
        val requests = testEngine.requests().first()
        assertEquals(Host.Domain("sts.us-west-2.amazonaws.com"), requests.actual.url.host)
    }

    @Test
    fun testPlatformRegion() = runTest {
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
                aws_access_key_id = AKID-Profile
                aws_secret_access_key = Profile-Secret
                """.trimIndent(),
            ),
        )

        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse(testArn))
        }

        ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClient = testEngine,
        ).resolve()

        testEngine.assertRequests()
        val requests = testEngine.requests().first()
        assertEquals(Host.Domain("sts.us-west-2.amazonaws.com"), requests.actual.url.host)
    }
}
