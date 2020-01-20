package auth.signer

expect class Sha256() {
    fun update(data: ByteArray)
    fun digest(): ByteArray
}