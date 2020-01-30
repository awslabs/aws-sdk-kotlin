package software.aws.kotlin.utils

expect object Base64 {
    fun encode(bytes: ByteArray): ByteArray
    fun decode(bytes: ByteArray): ByteArray
}