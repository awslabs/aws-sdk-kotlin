package aws.sdk.kotlin.s3.transfermanager

import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompleteMultipartUploadResponse
import aws.sdk.kotlin.services.s3.model.CopyObjectResponse
import aws.sdk.kotlin.services.s3.model.CopyPartResult
import aws.sdk.kotlin.services.s3.model.CreateMultipartUploadResponse
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.sdk.kotlin.services.s3.model.HeadBucketResponse
import aws.sdk.kotlin.services.s3.model.HeadObjectResponse
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Response
import aws.sdk.kotlin.services.s3.model.Object
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.sdk.kotlin.services.s3.model.UploadPartCopyResponse
import aws.sdk.kotlin.services.s3.model.UploadPartResponse
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.appendBytes
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
class DefaultS3TransferManagerTest {

    private lateinit var s3Client: S3Client

    private lateinit var s3TransferManager: S3TransferManager

    private lateinit var testUploadDirectory: Path

    private lateinit var testDownloadDirectory: Path

    @BeforeEach
    private fun setUp() {
        s3Client = mockk<S3Client>()
        s3TransferManager = runBlocking {
            S3TransferManager {
                chunkSize = 8000000
                s3 = s3Client
            }
        }
    }

    @AfterEach
    private fun finish() {
        unmockkAll()
    }

    private fun createUploadDirectory() {
        //        test directory structure like this:
        //        testUploadDirectory/
        //            file1.txt
        //            testUploadDirectory1/
        //                file2.png
        //                file3.jpeg
        //            testUploadDirectory2/
        val home: String = System.getProperty("user.home")
        val dir = Paths.get(home, "Downloads")
        testUploadDirectory = Files.createTempDirectory(dir, "testUploadDirectory")
        Files.createTempFile(testUploadDirectory, "file1", ".txt").appendBytes("ABCD".toByteArray())
        val testUploadDirectory1 = Files.createTempDirectory(testUploadDirectory, "testUploadDirectory1")
        Files.createTempFile(testUploadDirectory1, "file2", ".png").appendBytes("image".toByteArray())
        Files.createTempFile(testUploadDirectory1, "file3", ".jpeg")
        Files.createTempDirectory(testUploadDirectory, "testUploadDirectory2")
    }

    private fun createDownloadDirectory() {
        val home: String = System.getProperty("user.home")
        val dir = Paths.get(home, "Downloads")
        testDownloadDirectory = Files.createTempDirectory(dir, "testDownloadDirectory")
    }

    @Test
    fun testUploadDirectory() = runTest {
        createUploadDirectory()
        try {
            detectHeadBucket("bucket")
            coEvery {
                s3Client.putObject(
                    match {
                        it.bucket == "bucket" && it.key?.startsWith("folder") ?: false
                    }
                )
            } returns PutObjectResponse {}

            val operation = s3TransferManager.upload(testUploadDirectory.toString(), S3Uri("s3://bucket/folder"))
            assertNotNull(operation)
            operation.await()

            verifyHeadBucket("bucket")
            coVerify(exactly = 3) {
                s3Client.putObject(
                    match {
                        it.bucket == "bucket" && it.key?.startsWith("folder") ?: false
                    }
                )
            }
        } finally {
            testUploadDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testUploadLargeFile() = runTest {
        val testFile = RandomTempFile(10000000)
        try {
            detectHeadBucket("bucket")
            detectMultiPartUpload("bucket", "key", "123456")

            val operation = s3TransferManager.upload(testFile.path, S3Uri("bucket", "key"))
            assertNotNull(operation)
            operation.await()

            verifyHeadBucket("bucket")
            coVerify(exactly = 2) {
                s3Client.uploadPart(
                    match {
                        it.bucket == "bucket" && it.key == "key" && it.uploadId == "123456"
                    }
                )
            }
            coVerifyOrder {
                s3Client.createMultipartUpload(
                    match {
                        it.bucket == "bucket" && it.key == "key"
                    }
                )
                s3Client.uploadPart(
                    match {
                        it.bucket == "bucket" && it.key == "key" && it.uploadId == "123456"
                    }
                )
                s3Client.completeMultipartUpload(
                    match {
                        it.bucket == "bucket" && it.key == "key" && it.uploadId == "123456"
                    }
                )
            }
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testDownloadDirectory() = runTest {
        createDownloadDirectory()
        try {
            detectHeadBucket("bucket")
            detectListObjects("bucket", "folder/", "file")
            coEvery {
                s3Client.getObject(
                    match {
                        it.bucket == "bucket" && it.key?.startsWith("folder/") ?: false
                    },
                    any<suspend (GetObjectResponse) -> Any?>()
                )
            } returns Unit

            val operation = s3TransferManager.download(S3Uri("bucket", "folder/"), testDownloadDirectory.toString())
            assertNotNull(operation)
            operation.await()

            verifyHeadBucket("bucket")
            verifyListObjects("bucket", "folder/")
            coVerify(exactly = 2) { // size 0 file doesn't call getObject
                s3Client.getObject(
                    match {
                        it.bucket == "bucket" && it.key?.startsWith("folder/") ?: false
                    },
                    any<suspend (GetObjectResponse) -> Any?>()
                )
            }
        } finally {
            testDownloadDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testDownloadLargeFile() = runTest {
        createDownloadDirectory()
        try {
            detectHeadBucket("bucket")
            detectHeadLargeObject("bucket", "key")
            coEvery {
                s3Client.getObject(
                    match {
                        it.bucket == "bucket" && it.key == "key"
                    },
                    any<suspend (GetObjectResponse) -> Any?>()
                )
            } returns Unit

            val operation = s3TransferManager.download(S3Uri("bucket", "key"), testDownloadDirectory.toString())
            assertNotNull(operation)
            operation.await()

            verifyHeadBucket("bucket")
            coVerify(exactly = 2) {
                s3Client.getObject(
                    match {
                        it.bucket == "bucket" && it.key == "key"
                    },
                    any<suspend (GetObjectResponse) -> Any?>()
                )
            }
            coVerifyOrder {
                s3Client.headObject(
                    match {
                        it.bucket == "bucket" && it.key == "key"
                    }
                )
                s3Client.getObject(
                    match {
                        it.bucket == "bucket" && it.key == "key"
                    },
                    any<suspend (GetObjectResponse) -> Any?>()
                )
            }
        } finally {
            testDownloadDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testCopyDirectory() = runTest {
        detectHeadBucket("bucket1")
        detectHeadBucket("bucket2")
        detectListObjects("bucket1", "folder1/", "file")
        coEvery {
            s3Client.copyObject(
                match {
                    it.copySource?.startsWith("bucket1/folder1/file") ?: false && it.bucket == "bucket2" && it.key?.startsWith("folder2/file") ?: false
                }
            )
        } returns CopyObjectResponse {}

        val operation = s3TransferManager.copy(S3Uri("s3://bucket1/folder1/"), S3Uri("bucket2", "folder2/"))
        assertNotNull(operation)
        operation.await()

        verifyHeadBucket("bucket1")
        verifyHeadBucket("bucket2")
        verifyListObjects("bucket1", "folder1/")
        coVerify(exactly = 3) {
            s3Client.copyObject(
                match {
                    it.copySource?.startsWith("bucket1/folder1/file") ?: false && it.bucket == "bucket2" && it.key?.startsWith("folder2/file") ?: false
                }
            )
        }
    }

    @Test
    fun testCopyLargeObject() = runTest {
        detectHeadBucket("bucket1")
        detectHeadBucket("bucket2")
        detectHeadLargeObject("bucket1", "key1")
        detectMultiPartCopy("bucket1/key1", "bucket2", "key2", "123456")

        val operation = s3TransferManager.copy(S3Uri("bucket1", "key1"), S3Uri("s3://bucket2/key2"))
        assertNotNull(operation)
        operation.await()

        verifyHeadBucket("bucket1")
        verifyHeadBucket("bucket2")
        coVerifyOrder {
            s3Client.headObject(
                match {
                    it.bucket == "bucket1" && it.key == "key1"
                }
            )
            s3Client.createMultipartUpload(
                match {
                    it.bucket == "bucket2" && it.key == "key2"
                }
            )
            s3Client.uploadPartCopy(
                match {
                    it.copySource == "bucket1/key1" && it.bucket == "bucket2" && it.key == "key2" && it.uploadId == "123456"
                }
            )
            s3Client.completeMultipartUpload(
                match {
                    it.bucket == "bucket2" && it.key == "key2" && it.uploadId == "123456"
                }
            )
        }
        coVerify(exactly = 2) {
            s3Client.uploadPartCopy(
                match {
                    it.copySource == "bucket1/key1" && it.bucket == "bucket2" && it.key == "key2" && it.uploadId == "123456"
                }
            )
        }
    }

    private fun detectHeadBucket(bucket: String) {
        coEvery {
            s3Client.headBucket(
                match {
                    it.bucket == bucket
                }
            )
        } returns HeadBucketResponse {}
    }

    private fun detectHeadLargeObject(bucket: String, key: String) {
        coEvery {
            s3Client.headObject(
                match {
                    it.bucket == bucket && it.key == key
                }
            )
        } returns HeadObjectResponse {
            contentLength = 10000000L
        }
    }

    private fun detectListObjects(bucket: String, keyDirectory: String, fileNamePrefix: String) {
        val keyPrefix = Paths.get(keyDirectory, fileNamePrefix).toString()
        val objectList = mutableListOf<Object>()
        for (i in 0 until 3) {
            objectList.add(
                Object {
                    key = "$keyPrefix$i"
                    size = i.toLong()
                }
            )
        }

        coEvery {
            s3Client.listObjectsV2(
                match {
                    it.bucket == bucket && it.prefix == keyDirectory
                }
            )
        } returns ListObjectsV2Response {
            nextContinuationToken = null
            contents = objectList
        }
    }

    private fun detectMultiPartUpload(bucket: String, key: String, uploadId: String) {
        detectPartsTransfer(bucket, key, uploadId)
        coEvery {
            s3Client.uploadPart(
                match {
                    it.bucket == bucket && it.key == key && it.uploadId == uploadId
                }
            )
        } returns UploadPartResponse {
            eTag = "654321"
        }
    }

    private fun detectMultiPartCopy(copySource: String, bucket: String, key: String, uploadId: String) {
        detectPartsTransfer(bucket, key, uploadId)
        coEvery {
            s3Client.uploadPartCopy(
                match {
                    it.copySource == copySource && it.bucket == bucket && it.key == key && it.uploadId == uploadId
                }
            )
        } returns UploadPartCopyResponse {
            copyPartResult = CopyPartResult {
                eTag = "654321"
            }
        }
    }

    private fun detectPartsTransfer(bucketName: String, keyName: String, transferId: String) {
        coEvery {
            s3Client.createMultipartUpload(
                match {
                    it.bucket == bucketName && it.key == keyName
                }
            )
        } returns CreateMultipartUploadResponse {
            bucket = bucketName
            key = keyName
            uploadId = transferId
        }
        coEvery {
            s3Client.completeMultipartUpload(
                match {
                    it.bucket == bucketName && it.key == keyName && it.uploadId == transferId
                }
            )
        } returns CompleteMultipartUploadResponse {}
    }

    private fun verifyHeadBucket(bucket: String) {
        coVerify {
            s3Client.headBucket(
                match {
                    it.bucket == bucket
                }
            )
        }
    }

    private fun verifyListObjects(bucket: String, keyPrefix: String) {
        coVerify {
            s3Client.listObjectsV2(
                match {
                    it.bucket == bucket && it.prefix == keyPrefix
                }
            )
        }
    }
}
