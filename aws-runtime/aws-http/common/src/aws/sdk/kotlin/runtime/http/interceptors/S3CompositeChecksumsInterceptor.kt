package aws.sdk.kotlin.runtime.http.interceptors

import aws.smithy.kotlin.runtime.client.ProtocolResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.response.toBuilder
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlin.coroutines.coroutineContext

private const val CHECKSUM_HEADER_PREFIX = "x-amz-checksum-"

/**
 * Removes any checksum headers that contain composite checksums from an S3 response.
 */
public class S3CompositeChecksumsInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeDeserialization(context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>): HttpResponse {
        val response = context.protocolResponse.toBuilder()
        val logger = coroutineContext.logger<S3CompositeChecksumsInterceptor>()

        response.headers.removeCompositeChecksums(logger)

        return response.build()
    }
}

/**
 * Removes any checksum headers that contain composite checksums.
 */
internal fun HeadersBuilder.removeCompositeChecksums(logger: Logger): Unit =
    names().forEach { header ->
        if (header.startsWith(CHECKSUM_HEADER_PREFIX, ignoreCase = true) && get(header)!!.isCompositeChecksum()) {
            logger.warn { "S3 returned a composite checksum ($header), composite checksums are not supported, removing checksum" }
            remove(header)
        }
    }

/**
 * Verifies if a checksum is composite.
 */
private fun String.isCompositeChecksum(): Boolean {
    // Ends with "-#" where "#" is a number
    val regex = Regex("-(\\d)+$")
    return regex.containsMatchIn(this)
}
