package software.aws.kotlin.utils

import java.util.Base64

actual object Base64 {
    actual fun encode(bytes: ByteArray) = Base64.getEncoder().encode(bytes)

    actual fun decode(bytes: ByteArray) = Base64.getDecoder().decode(bytes)
}