package software.aws.kotlin.utils

const val LOOKUP = "0123456789abcdef";
fun ByteArray.toHexString(): String {
    return buildString(this.size * 2) {
        this@toHexString.forEach {
            val byte = it.toInt() and 0xFF
            append(LOOKUP[byte shr 4 and 0xf])
            append(LOOKUP[byte and 0xf])
        }
    }
}