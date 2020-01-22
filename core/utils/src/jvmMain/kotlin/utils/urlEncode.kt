package utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val ENCODED_CHARACTERS_PATTERN = Regex("""\+|\*|%7E|%2F""")

actual fun String.urlEncode(ignoreSlashes: Boolean): String {
    var encoded = URLEncoder.encode(this, StandardCharsets.UTF_8.name())
    val matcher = ENCODED_CHARACTERS_PATTERN.findAll(encoded)

    // TODO: This makes a lot of worthless StringBuilders, can we replace in 1 go using forEachIndexed
    matcher.forEach {
        val replacement = when (it.value) {
            "+" -> "%20"
            "*" -> "%2A"
            "%7E" -> "~"
            "" -> if (ignoreSlashes) "/" else null
            else -> null
        }

        replacement?.let { _ ->
            encoded = encoded.replaceRange(it.range, replacement)
        }
    }

    return encoded
}