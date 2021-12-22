/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.Filesystem
import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// TODO - refactor to make this work in common
class DefaultChainCredentialsProviderTest {

    class FsRootedAt(val root: File) : Filesystem {
        override val filePathSeparator: String = "/"
        override suspend fun readFileOrNull(path: String): ByteArray? {
            val realPath = Paths.get(root.path, path).toFile()
            return if (realPath.exists()) {
                withContext(Dispatchers.IO) {
                    realPath.readBytes()
                }
            } else {
                null
            }
        }
    }

    class DefaultChainPlatformProvider(
        private val env: Map<String, String>,
        private val fs: Filesystem
    ) : PlatformProvider, Filesystem by fs {
        override fun osInfo(): OperatingSystem = OperatingSystem(OsFamily.Linux, "test")
        override fun getProperty(key: String): String? = null
        override fun getenv(key: String): String? = env[key]
    }

    sealed class TestResult {
        abstract val name: String
        abstract val docs: String

        data class Ok(
            override val name: String,
            override val docs: String,
            val expected: Credentials
        ) : TestResult()

        data class ErrorContains(
            override val name: String,
            override val docs: String,
            val message: String
        ) : TestResult()
        companion object {

            fun fromJson(payload: String): TestResult {
                val obj = Json.parseToJsonElement(payload).jsonObject
                val name = checkNotNull(obj["name"]).jsonPrimitive.content
                val docs = checkNotNull(obj["docs"]).jsonPrimitive.content
                val result = checkNotNull(obj["result"]).jsonObject
                return when {
                    "Ok" in result -> {
                        val o = checkNotNull(result["Ok"]).jsonObject
                        val creds = Credentials(
                            checkNotNull(o["access_key_id"]).jsonPrimitive.content,
                            checkNotNull(o["secret_access_key"]).jsonPrimitive.content,
                            o["session_token"]?.jsonPrimitive?.content,
                            o["expiry"]?.jsonPrimitive?.longOrNull?.let { Instant.fromEpochSeconds(it) }
                        )
                        Ok(name, docs, creds)
                    }
                    "ErrorContains" in result -> ErrorContains(name, docs, checkNotNull(result["ErrorContains"]).jsonPrimitive.content)
                    else -> error("unrecognized result object: $result")
                }
            }
        }
    }

    data class TestCase(
        val expected: TestResult,
        val testPlatform: PlatformProvider,
        val testEngine: TestConnection,
    )

    fun makeTest(name: String): TestCase {
        val url = this::class.java.classLoader.getResource("default-provider-chain") ?: error("failed to load default-provider-chain test suite resource")
        val testSuiteDir = Paths.get(url.toURI()).toFile()
        val testDir = testSuiteDir.resolve(name)
        if (!testDir.exists()) error("$testDir does not exist")

        val testCaseFile = testDir.resolve("test-case.json")
        if (!testCaseFile.exists()) error("no test-case.json in $testDir")

        val testResult = TestResult.fromJson(testCaseFile.readText())

        val envFile = testDir.resolve("env.json")
        val env = if (envFile.exists()) {
            val el = Json.parseToJsonElement(envFile.readText())
            el.jsonObject.mapValues { it.value.jsonPrimitive.content }
        } else {
            emptyMap()
        }

        val httpTrafficFile = testDir.resolve("http-traffic.json")
        val testEngine = if (httpTrafficFile.exists()) {
            val traffic = httpTrafficFile.readText()
            TestConnection.fromJson(traffic)
        } else {
            TestConnection()
        }

        val fs = FsRootedAt(testDir.resolve("fs"))
        // TODO - support for system props
        val testProvider = DefaultChainPlatformProvider(env, fs)

        return TestCase(testResult, testProvider, testEngine)
    }

    /**
     * Execute a test from the default chain test suite
     * @param name The name of root directory for the test (from common/test-resources/default-provider-chain)
     */
    fun executeTest(name: String): Unit = runSuspendTest {
        val test = makeTest(name)
        println(test)
        val provider = DefaultChainCredentialsProvider(test.testPlatform, test.testEngine)
        val actual = runCatching { provider.getCredentials() }
        val expected = test.expected
        when {
            expected is TestResult.Ok && actual.isFailure -> error("expected success, got error: $actual")
            expected is TestResult.ErrorContains && actual.isSuccess -> error("expected error, succeeded: $actual")
            expected is TestResult.Ok && actual.isSuccess -> {
                // if the expected creds have no expiration, use that, otherwise assert they are the same
                // this is because the caching provider will expire even static creds after the given default timeout
                val actualCreds = actual.getOrThrow()

                val sanitizedExpiration = if (expected.expected.expiration == null) null else actualCreds.expiration
                val creds = actualCreds.copy(providerName = null, expiration = sanitizedExpiration)
                assertEquals(expected.expected, creds)
                // TODO - assert http traffic (to the extent we can)
            }
            expected is TestResult.ErrorContains && actual.isFailure -> {
                val ex = actual.exceptionOrNull()!!
                // the chain contains a generic exception when specific ones aren't found, but it
                // contains all of the suppressed exceptions along the way. Inspect them all and their causes.
                // In particular a test case only looks to verify a specific behavior and even though it
                // may fail at the correct spot, later providers may still be tried and also fail.
                val needle = expected.message
                val haystack = listOf(ex.message!!) + ex.suppressed.map { it.message!! } + ex.suppressed.mapNotNull { it.cause?.message }
                val expectedErrorFound = haystack.any { it.contains(needle) }
                assertTrue(expectedErrorFound, "`$needle` not found in any of the chain exception messages: $haystack")
            }
            else -> error("should not be able to get here")
        }
    }

    @Test
    fun testEcsAssumeRole() = executeTest("ecs_assume_role")

    @Test
    fun testEcsCredentials() = executeTest("ecs_credentials")

    @Test
    fun testImdsAssumeRole() = executeTest("imds_assume_role")

    @Test
    fun testImdsConfigWithNoCreds() = executeTest("imds_config_with_no_creds")

    @Test
    fun testImdsDefaultChainError() = executeTest("imds_default_chain_error")

    @Test
    fun testImdsDefaultChainRetries() = executeTest("imds_default_chain_retries")

    @Test
    fun testImdsDefaultChainSuccess() = executeTest("imds_default_chain_success")

    @Test
    fun testImdsDisabled() = executeTest("imds_disabled")

    @Test
    fun testImdsNoIamRole() = executeTest("imds_no_iam_role")

    @Test
    fun testImdsTokenFail() = executeTest("imds_token_fail")

    @Test
    fun testPreferEnvironment() = executeTest("prefer_environment")

    @Test
    fun testProfileName() = executeTest("profile_name")

    @Test
    fun testProfileOverridesWebIdentity() = executeTest("profile_overrides_web_identity")

    @Test
    fun testProfileStaticKeys() = executeTest("profile_static_keys")

    @Test
    fun testWebIdentitySourceProfileNoEnv() = executeTest("web_identity_source_profile_no_env")

    @Test
    fun testWebIdentityTokenEnv() = executeTest("web_identity_token_env")

    @Test
    fun testWebIdentityTokenInvalidJwt() = executeTest("web_identity_token_invalid_jwt")

    @Test
    fun testWebIdentityTokenProfile() = executeTest("web_identity_token_profile")

    @Test
    fun testWebIdentityTokenSourceProfile() = executeTest("web_identity_token_source_profile")
}
