package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Execute a command and return a pair containing the exit code and contents of either stdout or stderr.
 * If the exit code is 0, stdout will be returned. Otherwise, stderr will be returned.
 *
 * @param command the command to execute
 * @param platformProvider the platform provider which is used to determine the system's shell
 * @return Pair containing the command's exit code and either stdout or stderr
 */
internal actual suspend fun executeCommand(command: String, platformProvider: PlatformProvider): Pair<Int, String> {
    val cmd = ArrayList<String>()

    // add the platform's shell
    when (platformProvider.osInfo().family) {
        OsFamily.Windows -> {
            cmd.add("cmd.exe")
            cmd.add("/C")
        }
        else -> {
            cmd.add("sh")
            cmd.add("-c")
        }
    }

    // add the user-supplied command
    cmd.add(command)

    return withContext(Dispatchers.IO) {
        val process = ProcessBuilder().command(cmd).start()
        process.waitFor()

        Pair(
            process.exitValue(),
            if (process.exitValue() == 0) process.inputStream.bufferedReader().use { it.readText() }
            else process.errorStream.bufferedReader().use { it.readText() },
        )
    }
}
