package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.http.DeferredHeadersBuilder
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChecksumRemovalTest {
    @Test
    fun removeChecksumHeaders() {
        val headers = HeadersBuilder()

        headers.append("x-amz-checksum-crc32", "foo")
        headers.append("x-amz-checksum-sha256", "bar")

        assertTrue(
            headers.contains("x-amz-checksum-crc32"),
        )
        assertTrue(
            headers.contains("x-amz-checksum-sha256"),
        )

        headers.removeChecksumHeaders()

        assertFalse(
            headers.contains("x-amz-checksum-crc32"),
        )
        assertFalse(
            headers.contains("x-amz-checksum-sha256"),
        )
    }

    @Test
    fun removeChecksumTrailingHeaders() {
        val trailingHeaders = DeferredHeadersBuilder()

        trailingHeaders.add("x-amz-checksum-crc32", "foo")
        trailingHeaders.add("x-amz-checksum-sha256", "bar")

        assertTrue(
            trailingHeaders.contains("x-amz-checksum-crc32"),
        )
        assertTrue(
            trailingHeaders.contains("x-amz-checksum-sha256"),
        )

        trailingHeaders.removeChecksumTrailingHeaders()

        assertFalse(
            trailingHeaders.contains("x-amz-checksum-crc32"),
        )
        assertFalse(
            trailingHeaders.contains("x-amz-checksum-sha256"),
        )
    }

    @Test
    fun removeChecksumTrailingHeadersFromXAmzTrailer() {
        val headers = HeadersBuilder()

        headers.append("x-amz-trailer", "x-amz-checksum-crc32")
        headers.append("x-amz-trailer", "x-amz-trailing-header")

        val xAmzTrailer = headers.getAll("x-amz-trailer")

        assertTrue(
            xAmzTrailer?.contains("x-amz-checksum-crc32") ?: false,
        )
        assertTrue(
            xAmzTrailer?.contains("x-amz-trailing-header") ?: false,
        )

        headers.removeChecksumTrailingHeadersFromXAmzTrailer()

        assertFalse(
            xAmzTrailer?.contains("x-amz-checksum-crc32") ?: false,
        )
        assertTrue(
            xAmzTrailer?.contains("x-amz-trailing-header") ?: false,
        )
    }
}
