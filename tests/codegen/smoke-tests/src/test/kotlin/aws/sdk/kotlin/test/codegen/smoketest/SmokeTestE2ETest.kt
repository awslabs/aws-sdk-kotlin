/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.smoketests

import aws.sdk.kotlin.codegen.smoketests.AWS_SERVICE_FILTER
import aws.sdk.kotlin.codegen.smoketests.AWS_SKIP_TAGS
import org.gradle.testkit.runner.GradleRunner
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
        val smokeTestRunnerOutput = runSmokeTests("exceptionService", expectingFailure = true)

        assertContains(smokeTestRunnerOutput, "not ok ExceptionService ExceptionTest - no error expected from service")
        assertContains(smokeTestRunnerOutput, "#aws.smithy.kotlin.runtime.http.interceptors.SmokeTestsFailureException: Smoke test failed with HTTP status code: 400")
        assertContains(smokeTestRunnerOutput, "#\tat aws.smithy.kotlin.runtime.http.interceptors.SmokeTestsInterceptor.readBeforeDeserialization(SmokeTestsInterceptor.kt:19)")
        assertContains(smokeTestRunnerOutput, "#\tat aws.smithy.kotlin.runtime.http.interceptors.InterceptorExecutor.readBeforeDeserialization(InterceptorExecutor.kt:252)")
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
    expectingFailure: Boolean = false,
): String {
    val sdkRootDir = System.getProperty("user.dir") + "/../../../"

    val task = GradleRunner.create()
        .withProjectDir(File(sdkRootDir))
        .withArguments(
            "--stacktrace", // Make sure unexpected errors are debuggable
            "-Paws.kotlin.native=false", // FIXME: Remove `-Paws.kotlin.native=false` when Kotlin Native is ready
            ":tests:codegen:smoke-tests:services:$service:smokeTest",
        )
        .withEnvironment(envVars)

    val buildResult = if (expectingFailure) task.buildAndFail() else task.build()

    return buildResult.output
}
