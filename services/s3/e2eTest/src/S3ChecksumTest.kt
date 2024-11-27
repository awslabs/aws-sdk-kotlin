package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.e2etest.S3TestUtils.deleteBucketContents
import aws.sdk.kotlin.e2etest.S3TestUtils.deleteMultiPartUploads
import aws.sdk.kotlin.e2etest.S3TestUtils.getAccountId
import aws.sdk.kotlin.e2etest.S3TestUtils.getBucketByName
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.CompletedMultipartUpload
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromInputStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import java.io.File
import java.io.FileInputStream
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class S3ChecksumTest {
    private val client = S3Client { region = "us-west-2" }
    private val testBucket = "s3-test-bucket-ci-motorcade"
    private val testObject = "test-object"

    @BeforeAll
    private fun setUp(): Unit = runBlocking {
        val accountId = getAccountId()
        getBucketByName(client, testBucket, "us-west-2", accountId)
    }

    @AfterAll
    private fun cleanUp(): Unit = runBlocking {
        deleteMultiPartUploads(client, testBucket)
        deleteBucketContents(client, testBucket)
        client.close()
    }

    @Test
    @Order(1)
    fun testPutObject(): Unit = runBlocking {
        client.putObject {
            bucket = testBucket
            key = testObject
            body = ByteStream.fromString("Hello World")
        }
    }

    @Test
    @Order(2)
    fun testPutObjectWithEmptyBody(): Unit = runBlocking {
        client.putObject {
            bucket = testBucket
            key = testObject + UUID.randomUUID()
        }
    }

    @Test
    @Order(3)
    fun testPutObjectAwsChunkedEncoded(): Unit = runBlocking {
        val testString = "Hello World"
        val tempFile = File.createTempFile("test", ".txt").also {
            it.writeText(testString)
            it.deleteOnExit()
        }
        val inputStream = FileInputStream(tempFile)

        client.putObject {
            bucket = testBucket
            key = testObject + UUID.randomUUID()
            body = ByteStream.fromInputStream(inputStream, testString.length.toLong())
        }
    }

    @Test
    @Order(4)
    fun testMultiPartUpload(): Unit = runBlocking {
        // Parts need to be at least 5 MB
        val partOne = "Hello".repeat(1_048_576)
        val partTwo = "World".repeat(1_048_576)

        val testUploadId = client.createMultipartUpload {
            bucket = testBucket
            key = testObject
        }.uploadId

        val eTagPartOne = client.uploadPart {
            bucket = testBucket
            key = testObject
            partNumber = 1
            uploadId = testUploadId
            body = ByteStream.fromString(partOne)
        }.eTag

        val eTagPartTwo = client.uploadPart {
            bucket = testBucket
            key = testObject
            partNumber = 2
            uploadId = testUploadId
            body = ByteStream.fromString(partTwo)
        }.eTag

        client.completeMultipartUpload {
            bucket = testBucket
            key = testObject
            uploadId = testUploadId
            multipartUpload = CompletedMultipartUpload {
                parts = listOf(
                    CompletedPart {
                        partNumber = 1
                        eTag = eTagPartOne
                    },
                    CompletedPart {
                        partNumber = 2
                        eTag = eTagPartTwo
                    },
                )
            }
        }

        client.getObject(
            GetObjectRequest {
                bucket = testBucket
                key = testObject
            },
        ) {}
    }
}
