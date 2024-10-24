/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.credentials
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.http.interceptors.AwsBusinessMetric
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.collections.attributesOf
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import io.mockk.coEvery
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

        val testArn = "arn:aws:iam::1234567:role/test-role"
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
        val testArn = "arn:aws:iam::1234567:role/test-role"
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
        val testArn = "arn:aws:iam::1234567:role/test-role"
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
        val testArn = "arn:aws:iam::1234567:role/test-role"
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
        val testArn = "arn:aws:iam::1234567:role/test-role"
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

    @Test
    fun testAccountId() = runTest {
        val testProvider = TestPlatformProvider(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to """
                [default]
                aws_access_key_id = AKID-Default
                aws_secret_access_key = Default-Secret
                aws_account_id = 12345
                """.trimIndent(),
            ),
        )
        val testEngine = TestConnection()

        val provider = ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClient = testEngine,
        )
        val actual = provider.resolve()
        val expected = credentials("AKID-Default", "Default-Secret", accountId = "12345")
        assertEquals(expected, actual)
    }

    @Test
    fun profileCredentialsBusinessMetrics() = runTest {
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
        val attributes = ExecutionContext()
        provider.resolve(attributes)

        assertTrue(attributes.contains(BusinessMetrics))

        val actual = attributes[BusinessMetrics]
        val expected = setOf(
            AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE.identifier,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun assumeRoleWithSourceProfileBusinessMetrics() = runTest {
        val testArn = "arn:aws:iam::1234567:role/test-role"
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
        val attributes = ExecutionContext()
        provider.resolve(attributes)

        assertTrue(attributes.contains(BusinessMetrics))

        val actual = attributes[BusinessMetrics]
        val expected = setOf(
            AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_SOURCE_PROFILE.identifier,
            AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE.identifier,
            AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE.identifier,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun assumeRoleWithNamedProviderBusinessMetrics() = runTest {
        val testArn = "arn:aws:iam::1234567:role/test-role"
        val testProvider = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                "AWS_REGION" to "us-west-2",
                "AWS_ACCESS_KEY_ID" to "1",
                "AWS_SECRET_ACCESS_KEY" to "2",
            ),
            fs = mapOf(
                "config" to """
                [default]
                role_arn = $testArn
                credential_source = Environment
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
        val attributes = ExecutionContext()
        provider.resolve(attributes)

        assertTrue(attributes.contains(BusinessMetrics))

        val actual = attributes[BusinessMetrics]
        val expected = setOf(
            AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_NAMED_PROVIDER.identifier,
            AwsBusinessMetric.Credentials.CREDENTIALS_ENV_VARS.identifier,
            AwsBusinessMetric.Credentials.CREDENTIALS_STS_ASSUME_ROLE.identifier,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun processBusinessMetrics() = runTest {
        val testProvider = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to """
                [default]
                credential_process = awscreds-custom
                """.trimIndent(),
                "awscreds-custom" to "some-process",
            ),
        )
        val testEngine = TestConnection()
        val provider = ProfileCredentialsProvider(
            platformProvider = testProvider,
            httpClient = testEngine,
        )

        mockkStatic(::executeCommand)
        coEvery { executeCommand(any(), any(), any(), any(), any()) }.returns(
            Pair(
                0,
                """
            {
                "Version": 1,
                "AccessKeyId": "AccessKeyId",
                "SecretAccessKey": "SecretAccessKey",
                "SessionToken": "SessionToken"
            }
                """.trimIndent(),
            ),
        )

        val attributes = ExecutionContext()
        provider.resolve(attributes)

        assertTrue(attributes.contains(BusinessMetrics))

        val actual = attributes[BusinessMetrics]
        val expected = setOf(
            AwsBusinessMetric.Credentials.CREDENTIALS_PROFILE_PROCESS.identifier,
            AwsBusinessMetric.Credentials.CREDENTIALS_PROCESS.identifier,
        )
        assertEquals(expected, actual)
    }
}
