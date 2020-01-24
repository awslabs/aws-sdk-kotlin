package auth.signer

expect class Sha256() {
    fun update(data: ByteArray)
    fun digest(): ByteArray
    fun reset()
}

expect class HmacSha256(secret: ByteArray) {
    fun sign(data: ByteArray): ByteArray
}