package software.aws.kotlinsdk.auth

import java.io.File

/**
 * Platform function to pass a file to an input function as a sequence of lines.
 */
actual fun <T> platformFileReader(
    customFilePath: String?,
    block: (Sequence<String>) -> T
): T? {
    val resolvedProfilePath = File(customFilePath ?: resolveFilePath())

    if (!resolvedProfilePath.exists()) return null
    if (resolvedProfilePath.isDirectory || resolvedProfilePath.length() == 0L) {
        // TODO: Log here that credentials file is invalid
        return null
    }

    return resolvedProfilePath.useLines(block = block)
}

fun resolveFilePath() =
    "${System.getProperty("user.home")}${File.separator}.aws${File.separator}credentials"