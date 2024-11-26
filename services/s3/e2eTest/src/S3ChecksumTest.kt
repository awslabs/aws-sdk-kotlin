package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.e2etest.S3TestUtils.deleteBucketContents
import aws.sdk.kotlin.e2etest.S3TestUtils.deleteMultiPartUploads
import aws.sdk.kotlin.e2etest.S3TestUtils.getAccountId
import aws.sdk.kotlin.e2etest.S3TestUtils.getTestBucketByName
import aws.sdk.kotlin.runtime.auth.credentials.ProcessCredentialsProvider
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.CompletedMultipartUpload
import aws.sdk.kotlin.services.s3.model.CompletedPart
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
    private val s3West = S3Client {
        region = "us-west-2"
        credentialsProvider = ProcessCredentialsProvider("isengardcli credentials --awscli aws-kotlin-sdk+ci@amazon.com --role Admin")
    }
    private val checksumsTestBucket = "s3-test-bucket-ci-motorcade"
    private val testObject = "test-object"
    private lateinit var accountId: String
    private lateinit var usWestBucket: String

    @BeforeAll
    private fun setUp(): Unit = runBlocking {
        accountId = getAccountId()
        usWestBucket = getTestBucketByName(s3West, checksumsTestBucket, "us-west-2", accountId)
    }

    @AfterAll
    private fun cleanUp(): Unit = runBlocking {
        deleteMultiPartUploads(s3West, checksumsTestBucket)
        deleteBucketContents(s3West, checksumsTestBucket)
        s3West.close()
    }

    @Test
    @Order(1)
    fun testPutObject(): Unit = runBlocking {
        s3West.putObject {
            bucket = checksumsTestBucket
            key = testObject
            body = ByteStream.fromString("Hello World")
        }
    }

    @Test
    @Order(2)
    fun testPutObjectWithEmptyBody(): Unit = runBlocking {
        s3West.putObject {
            bucket = checksumsTestBucket
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

        s3West.putObject {
            bucket = checksumsTestBucket
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

        val testUploadId = s3West.createMultipartUpload {
            bucket = checksumsTestBucket
            key = testObject
        }.uploadId

        val eTagPartOne = s3West.uploadPart {
            bucket = checksumsTestBucket
            key = testObject
            partNumber = 1
            uploadId = testUploadId
            body = ByteStream.fromString(partOne)
        }.eTag

        val eTagPartTwo = s3West.uploadPart {
            bucket = checksumsTestBucket
            key = testObject
            partNumber = 2
            uploadId = testUploadId
            body = ByteStream.fromString(partTwo)
        }.eTag

        s3West.completeMultipartUpload {
            bucket = checksumsTestBucket
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

        // TODO: Get the object and make sure a composite checksum doesn't break us
    }
}
