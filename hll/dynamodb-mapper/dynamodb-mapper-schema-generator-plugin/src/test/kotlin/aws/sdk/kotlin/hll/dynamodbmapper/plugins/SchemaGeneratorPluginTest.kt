package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains

class SchemaGeneratorPluginTest {
    @TempDir
    lateinit var testProjectDir: File

    private lateinit var settingsFile: File
    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts")
        buildFile = File(testProjectDir, "build.gradle.kts")
    }

    @AfterEach
    fun cleanup() {
        if (settingsFile.exists()) {
            settingsFile.delete()
        }
        if (buildFile.exists()) {
            buildFile.delete()
        }
    }

    @Test
    fun `applies the plugin`() {
        val buildFileContent = """
         plugins {
            id("org.jetbrains.kotlin.jvm") version "2.0.0"
            id("aws.sdk.kotlin.hll.dynamodbmapper.schema.generator")
         }
         configure<aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPluginExtension>{ }
        """.trimIndent()

        buildFile.writeText(buildFileContent)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("--info", "build")
            .withPluginClasspath()
            .withGradleVersion("8.5")
            .forwardOutput()
            .build()

        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)
    }
}
