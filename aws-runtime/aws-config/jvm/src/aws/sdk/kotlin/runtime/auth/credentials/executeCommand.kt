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
internal actual suspend fun executeCommand(command: String, isWindows: Boolean): Pair<Int, String> {
    val cmd = ArrayList<String>()

    // add the platform's shell
    if (isWindows) {
        cmd.add("cmd.exe")
        cmd.add("/C")
    } else {
        cmd.add("sh")
        cmd.add("-c")
    }

    // add the user-supplied command
    cmd.add(command)

    val process = withContext(Dispatchers.IO) {
        ProcessBuilder().command(cmd).start()
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
