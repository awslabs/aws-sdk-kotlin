package aws.sdk.kotlin.runtime.http.interceptors

import aws.smithy.kotlin.runtime.http.HeadersBuilder
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class S3CompositeChecksumsInterceptorTest {
    @Test
    fun compositeChecksumRemoval() = runTest {
        val headers = HeadersBuilder()

        headers.append("x-amz-checksum-crc32", "foo-1")
        headers.append("x-amz-checksum-sha256", "bar")

        assertTrue(
            headers.contains("x-amz-checksum-crc32"),
        )
        assertTrue(
            headers.contains("x-amz-checksum-sha256"),
        )

        headers.removeCompositeChecksums(coroutineContext.logger<S3CompositeChecksumsInterceptor>())

        assertFalse(
            headers.contains("x-amz-checksum-crc32"),
        )
        assertTrue(
            headers.contains("x-amz-checksum-sha256"),
        )
    }
}
