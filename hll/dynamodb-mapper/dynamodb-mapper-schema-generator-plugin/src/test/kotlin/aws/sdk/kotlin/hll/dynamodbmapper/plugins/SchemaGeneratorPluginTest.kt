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
    private lateinit var runner: GradleRunner

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts")
        buildFile = File(testProjectDir, "build.gradle.kts")

        val buildFileContent = """
        plugins {
            id("org.jetbrains.kotlin.jvm") version "2.0.10"
            id("aws.sdk.kotlin.hll.dynamodbmapper.schema.generator")
        }
        """.trimIndent()
        buildFile.appendText(buildFileContent)

        runner = GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withGradleVersion("8.5") // TODO parameterize
            .forwardOutput()

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
    fun `configures the plugin`() {
        val buildFileContent = """
         import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses

         configure<aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPluginExtension>{
             generateBuilderClasses = GenerateBuilderClasses.ALWAYS
         }
         dynamoDbMapper {
             generateBuilderClasses = GenerateBuilderClasses.WHEN_REQUIRED
         }
        """.trimIndent()

        buildFile.appendText(buildFileContent)

        val result = runner
            .withArguments("--info", "build")
            .build()

        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)
    }
}
