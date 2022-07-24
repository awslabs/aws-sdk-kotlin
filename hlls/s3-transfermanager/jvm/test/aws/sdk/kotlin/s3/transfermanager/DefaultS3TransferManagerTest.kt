package aws.sdk.kotlin.s3.transfermanager

import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.services.s3.S3Client
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
class DefaultS3TransferManagerTest {

    @MockK
    lateinit var s3Client: S3Client

    @MockK
    private lateinit var s3TransferManager: S3TransferManager

    @BeforeAll
    fun setUp() {
        MockKAnnotations.init(this)
//        mockkObject(s3Client)
//        mockkStatic(S3Client::class)
//        s3Client = mockk<S3Client>()
        s3Client = spyk()
        s3TransferManager = runBlocking {
            S3TransferManager {
                chunkSize = 8000000
                s3 = s3Client
            }
        }
    }

    @AfterAll
    fun finish() {
        unmockkAll()
    }

//    TODO Need further investigation about mocking client response and lambda
//    @Test
//    fun testUpload() = runTest {
//        var testFile = RandomTempFile(6000000)
//        coEvery {
//            s3TransferManager.config.s3.putObject {
//                bucket = "bucket1"
//                key = "key1"
//                body = any()
//            }
//        } returns PutObjectResponse {}
//        var operation = s3TransferManager.upload(testFile.path.toString(), S3Uri("s3://bucket1/key1"))
//        operation.await()
//
//        coVerify {
//            s3TransferManager.config.s3.putObject {
//                bucket = "bucket1"
//                key = "key1"
//                body = any()
//            }
//        }
//        assertNotNull(operation)

//        testFile = RandomTempFile(10000000)
//
//        coEvery { s3Client.createMultipartUpload {
//            bucket = "bucket2"
//            key = "key2"
//        } } returns CreateMultipartUploadResponse {
//            bucket = "bucket2"
//            key = "key2"
//            uploadId = "123456"
//        }
//
//        coEvery { s3Client.uploadPart {
//            bucket = "bucket2"
//            key = "key2"
//            uploadId = "123456"
//            body = any()
//            partNumber = 1
//        } } returns UploadPartResponse {
//            eTag = "654321"
//        }
//        coEvery { s3Client.uploadPart {
//            bucket = "bucket2"
//            key = "key2"
//            uploadId = "123456"
//            body = any()
//            partNumber = 2
//        } } returns UploadPartResponse {
//            eTag = "654321"
//        }
//
//        operation = s3TransferManager.upload(testFile.path, S3Uri("bucket2", "key2"))
//        operation.await()
//
//        coVerify { s3Client.completeMultipartUpload {
//            bucket = "bucket2"
//            key = "key2"
//            uploadId = "123456"
//            multipartUpload = any()
//        } }
//
//        assertNotNull(operation)
//    }

//    @Test
//    fun testDownload() = runTest {
//        coEvery { s3Client.headBucket {
//            bucket = "bucket"
//        } } returns HeadBucketResponse {}
//
//        coEvery { s3Client.headObject {
//            bucket = "bucket"
//            key = "key"
//        } } returns HeadObjectResponse {
//            contentLength = 6000000
//        }
//
//        coEvery { s3Client.getObject(GetObjectRequest {
//            bucket = "bucket"
//            key = "key"
//        }) {}}
//    }

    @Test
    fun testUploadInvalidFrom() = runTest {
        assertFailsWith<IllegalArgumentException>("The upload is completed without throwing invalid from path error") {
            coroutineScope {
                s3TransferManager.upload("/Users/wty/Desktop/folder1/haha", S3Uri("s3://wty-bucket/key")).await()
            }
        }
    }
}
