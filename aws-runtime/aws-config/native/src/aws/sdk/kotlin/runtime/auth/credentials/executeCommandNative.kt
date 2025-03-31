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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
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
): Pair<Int, String> {
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

    cmd.add(command) // add the user-supplied command
    cmd.add("2>&1") // redirect stderr to stdout

    return withContext(SdkDispatchers.IO) {
        memScoped {
            val fp = popen(cmd.joinToString(" "), "r") ?: error("Failed to execute popen: $command")

            try {
                val output = buildString {
                    val buffer = ByteArray(maxOutputLengthBytes.toInt())

                    withTimeout(timeoutMillis) {
                        while (true) {
                            val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
                            append(input.toKString())

                            if (length > maxOutputLengthBytes) {
                                throw CredentialsProviderException("Process output exceeded limit of $maxOutputLengthBytes bytes")
                            }
                        }
                    }
                }

                val status = pclose(fp)
                status to output
            } catch (e: Exception) {
                pclose(fp)
                throw e
            }
        }
    }
}
