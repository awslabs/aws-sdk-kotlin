/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.test.codegen.smoketest

import aws.sdk.kotlin.codegen.smoketests.AWS_SERVICE_FILTER
import aws.sdk.kotlin.codegen.smoketests.AWS_SKIP_TAGS
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class SmokeTestE2ETest {
    @Test
    fun successService() {
        val smokeTestRunnerOutput = runSmokeTests("successService")

        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTest - no error expected from service")
        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTestWithTags - no error expected from service")
    }

    @Test
    fun failureService() {
        val smokeTestRunnerOutput = runSmokeTests("failureService")

        assertContains(smokeTestRunnerOutput, "ok FailureService FailuresTest - error expected from service")
    }

    @Test
    fun exceptionService() {
        val smokeTestRunnerOutput = runSmokeTests("exceptionService")

        assertContains(smokeTestRunnerOutput, "not ok ExceptionService ExceptionTest - no error expected from service")
        assertContains(smokeTestRunnerOutput, "#aws.smithy.kotlin.runtime.http.interceptors.SmokeTestsFailureException: Smoke test failed with HTTP status code: 400")
    }

    @Test
    fun successServiceSkipTags() {
        val envVars = mapOf(AWS_SKIP_TAGS to "success")
        val smokeTestRunnerOutput = runSmokeTests("successService", envVars)

        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTest - no error expected from service")
        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTestWithTags - no error expected from service # skip")
    }

    @Test
    fun successServiceServiceFilter() {
        val envVars = mapOf(AWS_SERVICE_FILTER to "Failure") // Only run tests for services with this SDK ID
        val smokeTestRunnerOutput = runSmokeTests("successService", envVars)

        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTest - no error expected from service # skip")
        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTestWithTags - no error expected from service # skip")
    }
}

private fun runSmokeTests(
    service: String,
    envVars: Map<String, String> = emptyMap(),
): String {
    val sdkRootDir = System.getProperty("user.dir") + "/../../../"
    val processBuilder =
        ProcessBuilder(
            "./gradlew",
            ":tests:codegen:smoke-tests:services:$service:smokeTest",
            // Make sure unexpected errors are debuggable
            "--stacktrace",
            // FIXME: Remove `-Paws.kotlin.native=false` when Kotlin Native is ready
            "-Paws.kotlin.native=false",
        )
            .directory(File(sdkRootDir))
            .redirectErrorStream(true)

    processBuilder.environment().putAll(envVars)

    val process = processBuilder.start()
    val output = process.inputStream.bufferedReader().use { it.readText() }

    return output
}
