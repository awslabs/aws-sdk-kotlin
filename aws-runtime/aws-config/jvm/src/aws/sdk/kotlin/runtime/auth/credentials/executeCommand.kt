package aws.sdk.kotlin.runtime.auth.credentials

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Execute a command and return a pair containing the exit code and contents of either stdout or stderr.
 * If the exit code is 0, stdout will be returned. Otherwise, stderr will be returned.
 *
 * @param command the command to execute
 * @return Pair containing the command's exit code and either stdout or stderr
 */
internal actual suspend fun executeCommand(command: String): Pair<Int, String> {
    val process = withContext(Dispatchers.IO) {
        Runtime.getRuntime().exec(command)
    }

    withContext(Dispatchers.IO) {
        process.waitFor()
    }

    return Pair(
        process.exitValue(),
        if (process.exitValue() == 0) process.inputStream.bufferedReader().use { it.readText() }
        else process.errorStream.bufferedReader().use { it.readText() },
    )
}
