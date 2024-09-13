import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SERVICE_FILTER
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SKIP_TAGS
import java.io.File
import kotlin.test.*

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
    fun successServiceSkipTags() {
        val envVars = mapOf(SKIP_TAGS to "success")
        val smokeTestRunnerOutput = runSmokeTests("successService", envVars)

        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTest - no error expected from service")
        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTestWithTags - no error expected from service # skip")
    }

    @Test
    fun successServiceServiceFilter() {
        val envVars = mapOf(SERVICE_FILTER to "Failure") // Only run tests for services with this SDK ID
        val smokeTestRunnerOutput = runSmokeTests("successService", envVars)

        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTest - no error expected from service # skip")
        assertContains(smokeTestRunnerOutput, "ok SuccessService SuccessTestWithTags - no error expected from service # skip")
    }
}

private fun runSmokeTests(service: String, envVars: Map<String, String> = emptyMap()): String {
    val sdkRootDir = System.getProperty("user.dir") + "/../../../"

    val output = StringBuilder()
    val smokeTests = ProcessBuilder()
        .command("./gradlew", ":tests:codegen:smoke-tests:services:$service:smokeTest")
        .directory(File(sdkRootDir))
        .redirectErrorStream(true)

    envVars.forEach { (key, value) ->
        smokeTests
            .environment()
            .put(key, value)
    }

    smokeTests
        .start()
        .inputStream
        .bufferedReader()
        .forEachLine { output.append("$it\n") }

    return output.toString()
}
