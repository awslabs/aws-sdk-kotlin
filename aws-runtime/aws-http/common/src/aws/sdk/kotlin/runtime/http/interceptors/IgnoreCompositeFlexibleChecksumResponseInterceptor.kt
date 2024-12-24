package aws.sdk.kotlin.runtime.http.interceptors

import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.http.interceptors.FlexibleChecksumsResponseInterceptor

/**
 * Variant of the [FlexibleChecksumsResponseInterceptor] where composite checksums are not validated
 */
public class IgnoreCompositeFlexibleChecksumResponseInterceptor(
    responseValidationRequired: Boolean,
    responseChecksumValidation: ResponseHttpChecksumConfig?,
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
