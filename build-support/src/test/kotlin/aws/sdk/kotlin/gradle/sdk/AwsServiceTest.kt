/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AwsServiceTest {

    val modelContents = """
        ${"$"}version: "2"
        namespace gradle.test

        use aws.api#service
        use aws.protocols#awsJson1_0

        @service(sdkId: "Test Gradle")
        @awsJson1_0
        service TestService {
            operations: [],
            version: "1-alpha"
        }
    """.trimIndent()

    private data class TestResult(
        val model: File,
        val actual: AwsService?,
    )

    private fun testWith(
        tempDir: File,
        bootstrap: BootstrapConfig,
    ): TestResult {
        val project = ProjectBuilder.builder()
            .build()
        project.extra.set("sdkVersion", "1.2.3")

        // NOTE: Model assembler requires the correct .json or .smithy extension for the file contents
        val model = tempDir.resolve("test-gradle.smithy")
        model.writeText(modelContents)

        val lambda = fileToService(project, bootstrap)
        val actual = lambda(model)
        return TestResult(model, actual)
    }

    @Test
    fun testFileToService(@TempDir tempDir: File) {
        val tests = listOf(
            BootstrapConfig.ALL,
            // filename
            BootstrapConfig("+test-gradle"),
            BootstrapConfig("test-gradle"),
            // artifact name
            BootstrapConfig("+testgradle"),
            BootstrapConfig("testgradle"),
            // protocol
            BootstrapConfig(null, "awsJson1_0"),
        )

        tests.forEach { bootstrap ->
            val result = testWith(tempDir, bootstrap)
            val expected = AwsService(
                "gradle.test#TestService",
                "aws.sdk.kotlin.services.testgradle",
                "1.2.3",
                result.model,
                "test-gradle",
                "Test Gradle",
                "1-alpha",
                "The AWS SDK for Kotlin client for Test Gradle",
            )
            assertEquals(expected, result.actual)
        }
    }

    @Test
    fun testFileToServiceExclude(@TempDir tempDir: File) {
        val tests = listOf(
            // explicit exclude
            BootstrapConfig("-test-gradle"),
            BootstrapConfig("-testgradle"),
            // explicit include without service under test
            BootstrapConfig("other"),
            // protocol exclude
            BootstrapConfig(null, "-awsJson1_0"),
        )

        tests.forEach { bootstrap ->
            val result = testWith(tempDir, bootstrap)
            assertNull(result.actual, "expected null for bootstrap with $bootstrap")
        }
    }
}
