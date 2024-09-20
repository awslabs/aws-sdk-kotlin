import org.gradle.tooling.GradleConnector
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SERVICE_FILTER
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SKIP_TAGS
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import kotlin.test.*

class SmokeTestE2ETest {
    @Test
    fun successService() {
        val smokeTestRunnerOutput = runSmokeTests()

        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTest - no error expected from service")
        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTestWithTags - no error expected from service")
    }

    @Test
    fun failureService() {
        val smokeTestRunnerOutput = runSmokeTests()

        assertContains(smokeTestRunnerOutput, "ok FailureService FailuresTest - error expected from service")
    }

    @Test
    fun successServiceSkipTags() {
        val envVars = mapOf(SKIP_TAGS to "success")
        val smokeTestRunnerOutput = runSmokeTests(envVars)

        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTest - no error expected from service")
        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTestWithTags - no error expected from service # skip")
    }

    @Test
    fun successServiceServiceFilter() {
        val envVars = mapOf(SERVICE_FILTER to "Failure") // Only run tests for services with this SDK ID
        val smokeTestRunnerOutput = runSmokeTests(envVars)

        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTest - no error expected from service # skip")
        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTestWithTags - no error expected from service # skip")
    }
}

private fun runSmokeTests(envVars: Map<String, String> = emptyMap()): String {
    val sdkRootDir = System.getProperty("user.dir") + "/../../../"
    val outputStream = ByteArrayOutputStream()
    val connector = GradleConnector.newConnector()
        .forProjectDirectory(File(sdkRootDir))
        .useDistribution(URI("https://services.gradle.org/distributions/gradle-8.5-bin.zip"))
        .connect()

    try {
        connector.use {
            it.newBuild()
                .forTasks("smokeTest")
                .setStandardOutput(outputStream)
                .setStandardError(outputStream)
                .setEnvironmentVariables(envVars)
                .run()
        }
    } catch (e: Exception) {
        throw Exception(e.message + "\n\n\n\n\n" + outputStream.toString(), e)
    }

    return outputStream.toString()
}
