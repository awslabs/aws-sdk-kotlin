package aws.sdk.kotlin.runtime.http.interceptors

import aws.smithy.kotlin.runtime.client.config.HttpChecksumConfigOption
import aws.smithy.kotlin.runtime.http.interceptors.FlexibleChecksumsResponseInterceptor

/**
 * S3 variant of the flexible checksum interceptor where composite checksums are not validated
 */
public class S3FlexibleChecksumResponseInterceptor(
    responseValidationRequired: Boolean,
    responseChecksumValidation: HttpChecksumConfigOption?,
) : FlexibleChecksumsResponseInterceptor(
    responseValidationRequired,
    responseChecksumValidation,
) {
    override fun ignoreChecksum(checksum: String): Boolean =
        checksum.isCompositeChecksum()
}

/**
 * Verifies if a checksum is composite.
 */
private fun String.isCompositeChecksum(): Boolean {
    // Ends with "-#" where "#" is a number
    val regex = Regex("-(\\d)+$")
    return regex.containsMatchIn(this)
}
