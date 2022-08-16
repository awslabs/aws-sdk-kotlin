package aws.sdk.kotlin.s3.transfermanager

import aws.sdk.kotlin.runtime.InternalSdkApi
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
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.appendBytes
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
                chunkSize = 100L
                s3 = s3Client
            }
        }
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

            val to = S3Uri("s3://bucket/folder")
            val operation = s3TransferManager.upload(testUploadDirectory.toString(), to)
            assertNotNull(operation)
            operation.await()

            verifyHeadBucket("bucket")
            verifyUploadDirectory(testUploadDirectory.toFile(), to)
        } finally {
            testUploadDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testUploadLargeFile() = runTest {
        val testFile = RandomTempFile(150L)
        try {
            detectHeadBucket("bucket")
            detectMultiPartUpload("bucket", "key", "123456")

            val operation = s3TransferManager.upload(testFile.path, S3Uri("bucket", "key"))
            assertNotNull(operation)
            operation.await()

            verifyHeadBucket("bucket")
            verifyUploadPartsOrder("bucket", "key", "123456", 2)
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testDownloadDirectory() = runTest {
        createDownloadDirectory()
        try {
            detectHeadBucket("bucket")
            detectListObjects("bucket", "folder/", "file", 3)
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
            for (i in 0 until 3) {
                verifyDownloadObject("bucket", "folder/file$i", i.toLong())
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
            detectHeadLargeObject("bucket", "key", 150L)
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
            verifyDownloadObject("bucket", "key", 150L)
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
        detectListObjects("bucket1", "folder1/", "file", 3)
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
        for (i in 0 until 3) {
            verifyCopyObject("bucket1/folder1/file$i", "bucket2", "folder2/file$i")
        }
    }

    @Test
    fun testCopyLargeObject() = runTest {
        detectHeadBucket("bucket1")
        detectHeadBucket("bucket2")
        detectHeadLargeObject("bucket1", "key1", 150L)
        detectMultiPartCopy("bucket1/key1", "bucket2", "key2", "123456")

        val from = S3Uri("bucket1", "key1")
        val to = S3Uri("s3://bucket2/key2")
        val operation = s3TransferManager.copy(from, to)
        assertNotNull(operation)
        operation.await()

        verifyHeadBucket("bucket1")
        verifyHeadBucket("bucket2")
        verifyCopyPartsOrder(from, to, "123456", 150L)
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

    private fun detectHeadLargeObject(bucket: String, key: String, size: Long) {
        coEvery {
            s3Client.headObject(
                match {
                    it.bucket == bucket && it.key == key
                }
            )
        } returns HeadObjectResponse {
            contentLength = size
        }
    }

    private fun detectListObjects(bucket: String, keyDirectory: String, fileNamePrefix: String, fileNum: Int) {
        val keyPrefix = Paths.get(keyDirectory, fileNamePrefix).toString()
        val objectList = mutableListOf<Object>()
        for (i in 0 until fileNum) {
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

    private fun verifyUploadDirectory(localFile: File, to: S3Uri) {
        if (localFile.isFile()) {
            verifyPutObject(to.bucket, to.key)
        } else if (localFile.isDirectory()) {
            val subFiles = localFile.listFiles()
            subFiles.forEach {
                val subKey = Paths.get(to.key, it.name).toString()
                val subTo = S3Uri(to.bucket, subKey)
                verifyUploadDirectory(it, subTo)
            }
        }
    }

    private fun verifyPutObject(bucket: String, key: String) {
        coVerify() {
            s3Client.putObject(
                match {
                    it.bucket == bucket && it.key == key
                }
            )
        }
    }

    private fun verifyUploadPartsOrder(bucket: String, key: String, uploadId: String, chunksNum: Int) {
        coVerifyOrder {
            s3Client.createMultipartUpload(
                match {
                    it.bucket == bucket && it.key == key
                }
            )
            for (i in 1 until chunksNum + 1) {
                s3Client.uploadPart(
                    match {
                        it.bucket == bucket && it.key == key && it.uploadId == uploadId && it.partNumber == i
                    }
                )
            }
            s3Client.completeMultipartUpload(
                match {
                    it.bucket == bucket && it.key == key && it.uploadId == uploadId
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

    @OptIn(InternalSdkApi::class)
    private fun verifyDownloadObject(bucket: String, key: String, fileSize: Long) {
        if (fileSize == 0L) {
            return
        }
        val chunkRanges = partition(fileSize, s3TransferManager.config.chunkSize)
        coVerifyOrder {
            chunkRanges.forEach { it ->
                val contentRange = "bytes=${it.start}-${it.endInclusive}"
                s3Client.getObject(
                    match {
                        it.bucket == bucket && it.key == key && it.range == contentRange
                    },
                    any<suspend (GetObjectResponse) -> Any?>()
                )
            }
        }
    }

    private fun verifyCopyObject(copySource: String, bucket: String, key: String) {
        coVerify {
            s3Client.copyObject(
                match {
                    it.copySource == copySource && it.bucket == bucket && it.key == key
                }
            )
        }
    }

    @OptIn(InternalSdkApi::class)
    private fun verifyCopyPartsOrder(from: S3Uri, to: S3Uri, uploadId: String, size: Long) {
        val chunkRanges = partition(size, s3TransferManager.config.chunkSize)
        coVerifyOrder {
            s3Client.headObject(
                match {
                    it.bucket == from.bucket && it.key == from.key
                }
            )
            s3Client.createMultipartUpload(
                match {
                    it.bucket == to.bucket && it.key == to.key
                }
            )
            chunkRanges.forEachIndexed { index, chunkRange ->
                val contentRange = "bytes=${chunkRange.start}-${chunkRange.endInclusive}"
                s3Client.uploadPartCopy(
                    match {
                        it.copySource == "${from.bucket}/${from.key}" && it.bucket == to.bucket && it.key == to.key && it.uploadId == uploadId &&
                            it.partNumber == index + 1 && it.copySourceRange == contentRange
                    }
                )
            }
            s3Client.completeMultipartUpload(
                match {
                    it.bucket == to.bucket && it.key == to.key && it.uploadId == uploadId
                }
            )
        }
    }
}
