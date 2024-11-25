import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.checksums.*
import aws.sdk.kotlin.test.checksums.model.ChecksumAlgorithm
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.httptest.TestEngine
import kotlinx.coroutines.runBlocking
import utils.HeaderReader
import utils.TrailerReader
import kotlin.test.Test
import kotlin.test.assertTrue

// TODO - Simplify

class ChecksumStreamingRequestTest {
    @Test
    fun crc32(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "content-encoding" to "aws-chunked",
                "x-amz-trailer" to "x-amz-checksum-crc32",
            ),
        )

        val trailerReader = TrailerReader(
            expectedTrailers = mapOf(
                "x-amz-checksum-crc32" to "i9aeUg==",
            ),
        )

        TestClient {
            interceptors = mutableListOf(headerReader, trailerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.httpChecksumStreamingOperation {
                body = ByteStream.fromString("Hello world")
                checksumAlgorithm = ChecksumAlgorithm.Crc32
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders &&
                trailerReader.containsExpectedTrailers,
        )
    }

    @Test
    fun crc32c(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "content-encoding" to "aws-chunked",
                "x-amz-trailer" to "x-amz-checksum-crc32c",
            ),
        )

        val trailerReader = TrailerReader(
            expectedTrailers = mapOf(
                "x-amz-checksum-crc32c" to "crUfeA==",
            ),
        )

        TestClient {
            interceptors = mutableListOf(headerReader, trailerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.httpChecksumStreamingOperation {
                body = ByteStream.fromString("Hello world")
                checksumAlgorithm = ChecksumAlgorithm.Crc32C
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders &&
                trailerReader.containsExpectedTrailers,
        )
    }

    @Test
    fun sha1(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "content-encoding" to "aws-chunked",
                "x-amz-trailer" to "x-amz-checksum-sha1",
            ),
        )

        val trailerReader = TrailerReader(
            expectedTrailers = mapOf(
                "x-amz-checksum-sha1" to "e1AsOh9IyGCa4hLN+2Od7jlnP14=",
            ),
        )

        TestClient {
            interceptors = mutableListOf(headerReader, trailerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.httpChecksumStreamingOperation {
                body = ByteStream.fromString("Hello world")
                checksumAlgorithm = ChecksumAlgorithm.Sha1
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders &&
                trailerReader.containsExpectedTrailers,
        )
    }

    @Test
    fun sha256(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "content-encoding" to "aws-chunked",
                "x-amz-trailer" to "x-amz-checksum-sha256",
            ),
        )

        val trailerReader = TrailerReader(
            expectedTrailers = mapOf(
                "x-amz-checksum-sha256" to "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=",
            ),
        )

        TestClient {
            interceptors = mutableListOf(headerReader, trailerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.httpChecksumStreamingOperation {
                body = ByteStream.fromString("Hello world")
                checksumAlgorithm = ChecksumAlgorithm.Sha256
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders &&
                trailerReader.containsExpectedTrailers,
        )
    }
}
