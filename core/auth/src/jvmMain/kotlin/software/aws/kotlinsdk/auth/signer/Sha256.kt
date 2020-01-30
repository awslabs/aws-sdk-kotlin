package software.aws.kotlin.auth.signer

import software.aws.kotlin.types.SdkException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual class Sha256 {
    private val messageDigest = try {
        MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
        throw SdkException("Unable to get SHA256 Function", e)
    }

    actual fun update(data: ByteArray) {
        messageDigest.update(data)
    }

    actual fun digest(): ByteArray {
        return messageDigest.digest()
    }

    actual fun reset() {
        messageDigest.reset()
    }
}

actual class HmacSha256 actual constructor(secret: ByteArray) {
    private val mac = try {
        val instance = Mac.getInstance("HmacSHA256")
        instance.init(SecretKeySpec(secret, "HmacSHA256"))
        instance
    } catch (e: NoSuchAlgorithmException) {
        throw SdkException("Unable to get SHA256 Function", e)
    }

    actual fun sign(data: ByteArray): ByteArray = mac.doFinal(data)
}