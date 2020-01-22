package utils

actual fun String.urlEncode(ignoreSlashes: Boolean): String {
    val encoded = js("encodeURIComponent(this)") as String
    return if (ignoreSlashes) {
        encoded.replace("%2F", "/")
    } else {
        encoded
    }
}