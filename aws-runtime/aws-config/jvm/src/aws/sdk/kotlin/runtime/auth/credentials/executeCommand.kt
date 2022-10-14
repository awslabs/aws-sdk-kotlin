package aws.sdk.kotlin.runtime.auth.credentials

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
