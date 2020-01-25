package auth.signer

import utils.isNode

external fun require(module: String): dynamic

actual open class Sha256 actual constructor() {
    private var sha: Hash = createSha()

    private fun createSha(): Hash {
        return if (isNode) {
            crypto().createHash("sha256")
        } else {
            error("Browser is not supported")
        }
    }

    actual fun update(data: ByteArray) {
        sha.update(data)
    }

    actual fun digest(): ByteArray = sha.digest()

    actual fun reset() {
        sha = createSha()
    }
}

actual class HmacSha256 actual constructor(secret: ByteArray) {
    private val hmac = if (isNode) {
        crypto().createHmac("sha256", secret)
    } else {
        error("Browser is not supported")
    }

    actual fun sign(data: ByteArray): ByteArray {
        hmac.update(data)
        return hmac.digest()
    }
}

private fun crypto() = js("require('crypto')").unsafeCast<Crypto>()

private external interface Crypto {
    fun createHash(algorithm: String): Hash
    fun createHmac(algorithm: String, key: ByteArray): Hmac
}

private external interface Hash {
    fun update(data: ByteArray): Hash
    fun digest(): ByteArray
}

private external interface Hmac {
    fun update(data: ByteArray): Hash
    fun digest(): ByteArray
}
