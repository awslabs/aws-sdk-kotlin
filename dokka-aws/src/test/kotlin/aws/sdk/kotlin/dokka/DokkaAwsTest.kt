/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.dokka

import org.jsoup.Jsoup
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class DokkaAwsTest {
    val dokkaOutputDir = File("build/dokka")

    @AfterEach
    fun cleanup() {
        if (dokkaOutputDir.exists()) {
            try {
                dokkaOutputDir.deleteRecursively()
                println("Successfully deleted dokka folder")
            } catch (e: Exception) {
                println("Failed to delete dokka folder: ${e.message}")
            }
        }
    }

    @Test
    fun testLoadScripts() {
        val testFile = File("build/dokka/html/index.html")

        assumeTrue(
            testFile.exists(),
            "Skipping test: test file does not exist",
        )

        val document = Jsoup.parse(testFile, "UTF-8")

        val expectedScripts = listOf(
            "awshome_s_code.js",
        )

        val scripts = document.head().select("script[src]")

        expectedScripts.forEach { expectedScript ->
            assertTrue(
                scripts.any { it.attr("src").endsWith(expectedScript) },
                "Expected script $expectedScript not found",
            )
        }
    }
}
