/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.io.internal.SdkDispatchers
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider
import kotlinx.cinterop.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun executeCommand(
    command: String,
    platformProvider: PlatformProvider,
    maxOutputLengthBytes: Long,
    timeoutMillis: Long,
    clock: Clock,
): Pair<Int, String> = withContext(SdkDispatchers.IO) {
    val pipeFd = IntArray(2)
    if (pipe(pipeFd.refTo(0)) != 0) {
        error("Failed to create pipe")
    }

    val pid = fork()
    if (pid == -1) {
        error("Failed to fork")
    }

    if (pid == 0) {
        // Child process
        close(pipeFd[0]) // Close read end

        // Pass stdout and stderr back to parent
        dup2(pipeFd[1], STDOUT_FILENO)
        dup2(pipeFd[1], STDERR_FILENO)
        close(pipeFd[1])

        val shell = when (platformProvider.osInfo().family) {
            OsFamily.Windows -> "cmd.exe"
            else -> "sh"
        }

        val shellArg = when (platformProvider.osInfo().family) {
            OsFamily.Windows -> "/C"
            else -> "-c"
        }

        val argv = memScoped { (arrayOf(shell, shellArg, command).map { it.cstr.ptr } + null).toCValues() }
        execvp(shell, argv)
        _exit(127) // If exec fails
    }

    // Parent process
    close(pipeFd[1]) // Close write end

    val output = buildString {
        val buffer = ByteArray(maxOutputLengthBytes.toInt())

        withTimeout(timeoutMillis) {
            while (true) {
                val bytesRead = read(pipeFd[0], buffer.refTo(0), buffer.size.toULong()).toInt()
                if (bytesRead <= 0) break

                append(buffer.decodeToString(0, bytesRead))

                if (length > maxOutputLengthBytes) {
                    close(pipeFd[0])
                    throw CredentialsProviderException("Process output exceeded limit of $maxOutputLengthBytes bytes")
                }
            }
        }
    }

    close(pipeFd[0])

    memScoped {
        val status = alloc<IntVar>()
        waitpid(pid, status.ptr, 0)
        val exitCode = when (platformProvider.osInfo().family) {
            OsFamily.Windows -> status.value
            else -> (status.value shr 8) and 0xFF
        }

        exitCode to output
    }
}
