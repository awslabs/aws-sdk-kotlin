package aws.sdk.kotlin.s3.transfermanager

import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.services.s3.S3Client
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultS3TransferManagerTest {

    @MockK
    lateinit var s3Client: S3Client

    private lateinit var s3TransferManager: S3TransferManager

    @BeforeAll
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(s3Client)
        s3TransferManager = runBlocking {
            S3TransferManager {
                chunkSize = 8000000
                s3 = s3Client
            }
        }
    }

//    TODO Need further investigation about mocking client response and lambda
//    @Test
//    fun testUpload() = runTest {
//        val defaultPutObjectResponse = PutObjectResponse {}
//        var testFile = RandomTempFile(6000000)
////        coEvery {
////            s3Client.putObject {
////                body = ByteStream.fromFile(testFile)
////            }
////        } returns defaultPutObjectResponse
//
//        var operation = s3TransferManager.upload(testFile.path, S3Uri("s3://bucket/key"))
//        assertNotNull(operation)
//
//        testFile = RandomTempFile(10000000)
//        val createMultipartUploadRequest = CreateMultipartUploadRequest {
//            bucket = "bucket"
//            key = "key"
//        }
//
//        val createMultipartUploadResponse = CreateMultipartUploadResponse {
//            bucket = "bucket"
//            key = "key"
//            uploadId = "123456"
//        }
//
//        val uploadPartRequest = UploadPartRequest {
//            bucket = "bucket"
//            key = "key"
//            uploadId = createMultipartUploadResponse.uploadId
//        }
//
//        val uploadPartResponse = UploadPartResponse {
//            eTag = "654321"
//        }
//
//        val completeMultipartUploadRequest = CompleteMultipartUploadRequest {
//            bucket = "bucket"
//            key = "key"
//            uploadId = createMultipartUploadResponse.uploadId
//        }
//
//        coEvery { s3Client.createMultipartUpload(createMultipartUploadRequest) } returns createMultipartUploadResponse
//
//        coEvery { s3Client.uploadPart(uploadPartRequest) } returns uploadPartResponse
//
//        operation = s3TransferManager.upload(testFile.path, S3Uri("bucket", "key"))
//
//        coVerify { s3Client.completeMultipartUpload(completeMultipartUploadRequest) }
//
//        assertNotNull(operation)
//    }

    @Test
    fun testUploadInvalidFrom() = runTest {
        assertFailsWith<IllegalArgumentException>("From path is invalid") {
            s3TransferManager.upload("/Users/wty/Desktop/folder1/haha", S3Uri("S3://wty-bucket/key"))
        }
    }
}
