package auth.signer

import types.SdkException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

actual class Sha256 {
    val messageDigest = try {
        MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
        throw SdkException("Unable to get SHA256 Function", e)
    }

    actual fun update(data: ByteArray) {
    }

    actual fun digest(): ByteArray {
        TODO("Not yet implemented")
    }
}