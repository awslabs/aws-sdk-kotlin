import aws.sdk.kotlin.runtime.auth.credentials.executeCommand
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExecuteCommandTest {
    val provider = PlatformProvider.System

    val platformNewline = when (provider.osInfo().family) {
        OsFamily.Windows -> "\r\n"
        else -> "\n"
    }

    @Test
    fun testExecuteCommand() = runTest {
        val command = "echo Hello, World!"

        val result = executeCommand(
            command = command,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(0, result.first)
        assertEquals("Hello, World!$platformNewline", result.second)
    }

    @Test
    fun testExecutionTimedOut() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            executeCommand(
                command = "this won't be executed",
                platformProvider = provider,
                maxOutputLengthBytes = 1024L,
                timeoutMillis = 0,
            )
        }
    }

    @Test
    fun testTooManyBytes() = runTest {
        val command = "echo ${"Hello! ".repeat(500)}"

        val ex = assertFailsWith<CredentialsProviderException> {
            executeCommand(
                command = command,
                platformProvider = provider,
                maxOutputLengthBytes = 1024L,
                timeoutMillis = 1000,
            )
        }

        assertEquals("Process output exceeded limit of 1024 bytes", ex.message)
    }

    @Test
    fun testCommandHasQuotes() = runTest {
        val command = """echo \"Hello, in quotes!\""""

        val result = executeCommand(
            command = command,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(0, result.first)
        assertEquals(""""Hello, in quotes!"$platformNewline""", result.second)
    }

    @Test
    fun testMultipleArgumentCommand() = runTest {
        val command = when (provider.osInfo().family) {
            OsFamily.Windows -> "powershell -Command \"& {Write-Host 'Arg1'; Write-Host 'Arg2'; Write-Host 'Arg3'}\""
            else -> "printf '%s\\n%s\\n%s\\n' 'Arg1' 'Arg2' 'Arg3'"
        }

        val result = executeCommand(
            command = command,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(0, result.first)
        assertEquals("Arg1\nArg2\nArg3\n", result.second)
    }

    @Test
    fun testErrorReturnsStderr() = runTest {
        val errorCommand = when (provider.osInfo().family) {
            OsFamily.Windows -> "echo \"Error message\" 1>&2 & exit /b 13"
            else -> "echo 'Error message' >&2; exit 13"
        }

        val result = executeCommand(
            command = errorCommand,
            platformProvider = provider,
            maxOutputLengthBytes = 1024L,
            timeoutMillis = 1000,
        )

        assertEquals(13, result.first)
        assertEquals("Error message$platformNewline", result.second)
    }
}
