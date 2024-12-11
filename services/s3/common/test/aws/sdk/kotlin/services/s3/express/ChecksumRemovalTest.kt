package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.http.DeferredHeadersBuilder
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChecksumRemovalTest {
    // Header capitalization shouldn't matter
    private val crc32Header = "x-aMz-cHeCkSum-cRC32"
    private val sha256Header = "X-Amz-ChEcKsum-sHa256"

    @Test
    fun removeChecksumHeaders() {
        val headers = HeadersBuilder()

        headers.append(crc32Header, "foo")
        headers.append(sha256Header, "bar")

        assertTrue(
            headers.contains(crc32Header),
        )
        assertTrue(
            headers.contains(sha256Header),
        )

        headers.removeChecksumHeaders()

        assertFalse(
            headers.contains(crc32Header),
        )
        assertFalse(
            headers.contains(sha256Header),
        )
    }

    @Test
    fun removeChecksumTrailingHeaders() {
        val trailingHeaders = DeferredHeadersBuilder()

        trailingHeaders.add(crc32Header, "foo")
        trailingHeaders.add(sha256Header, "bar")

        assertTrue(
            trailingHeaders.contains(crc32Header),
        )
        assertTrue(
            trailingHeaders.contains(sha256Header),
        )

        trailingHeaders.removeChecksumTrailingHeaders()

        assertFalse(
            trailingHeaders.contains(crc32Header),
        )
        assertFalse(
            trailingHeaders.contains(sha256Header),
        )
    }

    @Test
    fun removeChecksumTrailingHeadersFromXAmzTrailer() {
        val headers = HeadersBuilder()

        headers.append("x-amz-trailer", crc32Header)
        headers.append("x-amz-trailer", "x-amz-trailing-header")

        val xAmzTrailer = headers.getAll("x-amz-trailer")

        assertTrue(
            xAmzTrailer?.contains(crc32Header) ?: false,
        )
        assertTrue(
            xAmzTrailer?.contains("x-amz-trailing-header") ?: false,
        )

        headers.removeChecksumTrailingHeadersFromXAmzTrailer()

        assertFalse(
            xAmzTrailer?.contains(crc32Header) ?: false,
        )
        assertTrue(
            xAmzTrailer?.contains("x-amz-trailing-header") ?: false,
        )
    }
}
