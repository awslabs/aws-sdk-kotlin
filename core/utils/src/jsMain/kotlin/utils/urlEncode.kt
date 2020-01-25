package utils

actual fun String.urlEncode(ignoreSlashes: Boolean): String {
    val encoded = encodeURIComponent(this)
    return if (ignoreSlashes) {
        encoded.replace("%2F", "/")
    } else {
        encoded
    }
}

private external fun encodeURIComponent(string: String): String