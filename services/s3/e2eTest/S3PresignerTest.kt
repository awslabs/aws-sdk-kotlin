package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presign
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.http.response.complete
import aws.smithy.kotlin.runtime.http.sdkHttpClient
import aws.smithy.kotlin.runtime.http.toByteStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3PresignerTest {
    companion object {
        const val DEFAULT_REGION = "us-east-2"
    }

    private val client = S3Client {
        region = DEFAULT_REGION
    }

    private lateinit var testBucket: String

    @BeforeAll
    private fun createResources(): Unit = runBlocking {
        testBucket = S3TestUtils.getTestBucket(client)
    }

    @AfterAll
    private fun cleanup() = runBlocking {
        S3TestUtils.deleteBucketAndAllContents(client, testBucket)
    }

    @Test
    fun testPutObjectPresigner() = runSuspendTest {
        val contents = "presign-test"
        val keyName = "put-obj-from-memory-presigned.txt"

        val presignedRequest = PutObjectRequest {
            bucket = testBucket
            key = keyName
        }.presign(client.config, 60)

        S3TestUtils.responseCodeFromPut(presignedRequest, contents)

        val req = GetObjectRequest {
            bucket = testBucket
            key = keyName
        }
        val roundTrippedContents = client.getObject(req) { it.body?.decodeToString() }

        assertEquals(contents, roundTrippedContents)
    }

    @Test
    fun testGetObjectPresigner() = runSuspendTest {
        val contents = "presign-test"
        val keyName = "put-obj-from-memory-presigned.txt"

        client.putObject {
            bucket = testBucket
            key = keyName
            body = ByteStream.fromString(contents)
        }

        val presignedRequest = GetObjectRequest {
            bucket = testBucket
            key = keyName
        }.presign(client.config, 60)

        CrtHttpEngine().use { engine ->
            val httpClient = sdkHttpClient(engine)

            val call = httpClient.call(presignedRequest)
            call.complete()

            assertEquals(200, call.response.status.value)
            val body = call.response.body.toByteStream()?.decodeToString()
            assertEquals(contents, body)
        }
    }
}
