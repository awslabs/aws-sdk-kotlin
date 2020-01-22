package utils

fun isUsingStandardPort(protocol: String, port: Int?): Boolean {
    if (port == null || port == -1) {
        return true
    }

    val scheme = protocol.toLowerCase()

    return (scheme == "http" && port == 80) || (scheme == "https" && port == 443)
}