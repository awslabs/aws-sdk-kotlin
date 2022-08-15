package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.s3.transfermanager.S3TransferManager
import aws.sdk.kotlin.s3.transfermanager.data.Progress
import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.s3.transfermanager.ensureEndsWith
import aws.sdk.kotlin.s3.transfermanager.headObjectOrNull
import aws.sdk.kotlin.s3.transfermanager.listener.ProgressListener
import aws.sdk.kotlin.s3.transfermanager.partition
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.paginators.listObjectsV2Paginated
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.appendBytes
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for bucket operations and presigner
 */

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3TransferManagerIntegrationTest {

    // create and operate all through user's S3 TransferManager and Client
    private val s3TransferManager = runBlocking {
        S3TransferManager {
            chunkSize = 8000000
            s3 = runBlocking {
                S3Client.fromEnvironment {}
            }
        }
    }

    private lateinit var testBucket: String

    private lateinit var backUpBucket: String

    private lateinit var testUploadDirectory: Path

    private lateinit var testDownloadDirectory: Path

    @BeforeEach
    private fun createResources(): Unit = runBlocking {
        testBucket = S3TransferManagerTestUtils.getTestBucket(s3TransferManager.config.s3)
    }

    @AfterEach
    private fun cleanup() = runBlocking {
        S3TransferManagerTestUtils.deleteBucketAndAllContents(s3TransferManager.config.s3, testBucket)
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

    private fun createBackUpBucket() = runBlocking {
        backUpBucket = S3TransferManagerTestUtils.getBucketWithPrefix(s3TransferManager.config.s3, "s3-backup-bucket-")
    }

    private fun deleteBackUpBucket() = runBlocking {
        S3TransferManagerTestUtils.deleteBucketAndAllContents(s3TransferManager.config.s3, backUpBucket)
    }

    @Test
    fun testUpload() = runTest {
        createUploadDirectory()
        val keyPrefix = "folder1"
        val toUri = S3Uri(testBucket, keyPrefix)
        try {
            val progressListener = TestingProgressListener()
            val operation = s3TransferManager.upload(testUploadDirectory.toString(), toUri, progressListener)
            assertNotNull(operation, "The transfer manager didn't start directory upload")
            operation.await()
            checkTransfer(testUploadDirectory.toFile(), toUri, operation.progress!!, 3L, 9L, 2L)
        } finally {
            testUploadDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testUploadLargeFile() = runTest {
        val testLargeFile = RandomTempFile(10000000)
        val largeFileKey = "largefile"
        val toUri = S3Uri(testBucket, largeFileKey)
        try {
            val progressListener = TestingProgressListener()
            val operation = s3TransferManager.upload(testLargeFile.path, toUri, progressListener)
            assertNotNull(operation, "The transfer manager didn't start parts upload")
            operation.await()
            checkTransfer(testLargeFile, toUri, operation.progress!!, 1L, 10000000L, 2L)
        } finally {
            testLargeFile.deleteRecursively()
        }
    }

    /**
     * overload function used to check upload or copy's content and progress
     */
    private suspend fun checkTransfer(
        localFile: File,
        s3Uri: S3Uri,
        progress: Progress,
        totalFiles: Long,
        totalBytes: Long,
        totalChunks: Long
    ) {
        assertTrue(checkUploadContent(localFile, s3Uri), "Uploaded files' content doesn't match original files")
        progress.checkProgress(totalFiles, totalBytes, totalChunks)
    }

    private suspend fun checkUploadContent(localFile: File, to: S3Uri): Boolean {
        when {
            localFile.isFile -> {
                return chunksCompare(localFile, to)
            }

            localFile.isDirectory -> {
                val subFiles = localFile.listFiles()
                // check empty folder not uploaded
                val listObjectsResponse = s3TransferManager.config.s3.listObjectsV2 {
                    bucket = to.bucket
                    prefix = to.key
                }
                if (subFiles.isEmpty() && listObjectsResponse.keyCount > 0) {
                    return false
                }
                subFiles.forEach {
                    val subKey = Paths.get(to.key, it.name).toString()
                    val subTo = S3Uri(to.bucket, subKey)
                    if (!checkUploadContent(it, subTo)) {
                        return false
                    }
                }
                return true
            }

            else -> return true // there might be DS data that doesn't affect test
        }
    }

    @Test
    fun testUploadInvalidFrom() = runTest {
        assertFailsWith<IllegalArgumentException>("The upload is completed without throwing invalid from path error") {
            coroutineScope {
                val randomSuffix = "${Random.nextLong(Long.MAX_VALUE)}/${Random.nextInt(Int.MAX_VALUE)}"
                val badPath = "/Users/$randomSuffix/Desk/haha"
                s3TransferManager.upload(badPath, S3Uri("s3://wty-bucket/key")).await()
            }
        }
    }

    @Test
    fun testUploadInvalidToBucket() = runTest {
        createUploadDirectory()
        try {
            assertFailsWith<IllegalArgumentException>("The upload is completed without throwing invalid to bucket error") {
                coroutineScope {
                    val randomSuffix = Random.nextLong(Long.MAX_VALUE)
                    val badBucketUri = S3Uri("s3://$testBucket$randomSuffix/key")
                    s3TransferManager.upload(testUploadDirectory.toString(), badBucketUri).await()
                }
            }
        } finally {
            testUploadDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testDownload() = runTest {
        createUploadDirectory()
        val s3Uri = S3Uri(testBucket, "folder1")
        try {
            val uploadOperation = s3TransferManager.upload(testUploadDirectory.toString(), s3Uri)
            uploadOperation.await()
        } finally {
            testUploadDirectory.toFile().deleteRecursively()
        }

        createDownloadDirectory()
        try {
            val progressListener = TestingProgressListener()
            val downloadOperation = s3TransferManager.download(s3Uri, testDownloadDirectory.toString(), progressListener)
            assertNotNull(downloadOperation, "The transfer manager didn't start directory download")
            downloadOperation.await()
            checkTransfer(s3Uri, testDownloadDirectory.toFile(), downloadOperation.progress!!, 3L, 9L, 2L)
        } finally {
            testDownloadDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testDownloadLargeFile() = runTest {
        val testLargeFile = RandomTempFile(10000000)
        val largeFileKey = "largefile"
        val s3Uri = S3Uri(testBucket, largeFileKey)

        try {
            val uploadOperation = s3TransferManager.upload(testLargeFile.path, s3Uri)
            uploadOperation.await()
        } finally {
            testLargeFile.deleteRecursively()
        }

        createDownloadDirectory()
        try {
            val progressListener = TestingProgressListener()
            val downloadOperation = s3TransferManager.download(s3Uri, testDownloadDirectory.toString(), progressListener)
            assertNotNull(downloadOperation)
            downloadOperation.await()
            val downloadFile = Paths.get(testDownloadDirectory.toString(), largeFileKey).toFile()
            checkTransfer(s3Uri, downloadFile, downloadOperation.progress!!, 1L, 10000000L, 2L)
        } finally {
            testDownloadDirectory.toFile().deleteRecursively()
        }
    }

    private suspend fun checkTransfer(
        s3Uri: S3Uri,
        localFile: File,
        progress: Progress,
        totalFiles: Long,
        totalBytes: Long,
        totalChunks: Long
    ) {
        assertTrue(checkDownloadContent(s3Uri, localFile), "Downloaded files' content doesn't match original files")
        progress.checkProgress(totalFiles, totalBytes, totalChunks)
    }

    @OptIn(InternalSdkApi::class)
    private suspend fun checkDownloadContent(from: S3Uri, localFile: File): Boolean {
        if (!from.key.endsWith('/')) {
            s3TransferManager.config.s3.headObjectOrNull(from)?.let { headObjectResponse ->
                return chunksCompare(localFile, from)
            }
        }

        val keyPrefix = from.key.ensureEndsWith('/')
        val response = s3TransferManager.config.s3.listObjectsV2Paginated {
            bucket = from.bucket
            prefix = keyPrefix
        }
        var checkResult = true
        response // Flow<ListObjectsV2Response>, a collection of pages
            .transform { it.contents?.forEach { obj -> emit(obj) } }
            .collect { obj ->
                val key = obj.key!!
                val subFrom = S3Uri(from.bucket, key)
                val keySuffix = key.substringAfter(keyPrefix)
                val subFile = Paths.get(localFile.toString(), keySuffix).toFile()
                if (!checkDownloadContent(subFrom, subFile)) {
                    checkResult = false
                    return@collect
                }
            }

        return checkResult
    }

    @Test
    fun testDownloadInvalidFromBucket() = runTest {
        assertFailsWith<IllegalArgumentException>("The download is completed without throwing from bucket error") {
            coroutineScope {
                val randomSuffix = Random.nextLong(Long.MAX_VALUE)
                val badBucketUri = S3Uri("s3://$testBucket$randomSuffix/folder1")
                s3TransferManager.download(badBucketUri, "/Users/wty/Desktop/folder1/haha").await()
            }
        }
    }

    @Test
    fun testDownloadInvalidFromKey() = runTest {
        assertFailsWith<IllegalArgumentException>("The download is completed without throwing from key error") {
            coroutineScope {
                val randomKey = "${Random.nextLong(Long.MAX_VALUE)}/${Random.nextInt(Int.MAX_VALUE)}"
                val badFromUri = S3Uri(testBucket, randomKey)
                s3TransferManager.download(badFromUri, "/Users/wty/Desktop/folder1/haha").await()
            }
        }
    }

    @Test
    fun testCopyInSingleBucket() = runTest {
        createUploadDirectory()
        val sourceUri = S3Uri(testBucket, "folder1")

        try {
            val uploadOperation = s3TransferManager.upload(testUploadDirectory.toString(), sourceUri)
            uploadOperation.await()
            val destUri = S3Uri(testBucket, "folder/folder1")
            val progressListener = TestingProgressListener()
            val copyOperation = s3TransferManager.copy(sourceUri, destUri, progressListener)
            assertNotNull(copyOperation)
            copyOperation.await()
            checkTransfer(testUploadDirectory.toFile(), destUri, copyOperation.progress!!, 3L, 9L, 2L)
        } finally {
            testUploadDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun testCopyLargeObjectInSingleBucket() = runTest {
        val testLargeFile = RandomTempFile(10000000)
        val largeFileKey = "largefile"
        val sourceUri = S3Uri(testBucket, largeFileKey)
        try {
            val uploadOperation = s3TransferManager.upload(testLargeFile.path, sourceUri)
            uploadOperation.await()
            val destUri = S3Uri(testBucket, "folder/$largeFileKey")
            val progressListener = TestingProgressListener()
            val copyOperation = s3TransferManager.copy(sourceUri, destUri, progressListener)
            assertNotNull(copyOperation)
            copyOperation.await()
            checkTransfer(testLargeFile, destUri, copyOperation.progress!!, 1L, 10000000L, 2L)
        } finally {
            testLargeFile.deleteRecursively()
        }
    }

    @Test
    fun testCopyBetweenBuckets() = runTest {
        createUploadDirectory()
        val sourceUri = S3Uri(testBucket, "folder1")

        try {
            val uploadOperation = s3TransferManager.upload(testUploadDirectory.toString(), sourceUri)
            uploadOperation.await()
        } catch (e: S3Exception) {
            testUploadDirectory.toFile().deleteRecursively()
            throw e
        }

        createBackUpBucket()
        val destUri = S3Uri(backUpBucket, "folder/folder1")
        try {
            val progressListener = TestingProgressListener()
            val copyOperation = s3TransferManager.copy(sourceUri, destUri, progressListener)
            assertNotNull(copyOperation)
            copyOperation.await()
            checkTransfer(testUploadDirectory.toFile(), destUri, copyOperation.progress!!, 3L, 9L, 2L)
        } finally {
            testUploadDirectory.toFile().deleteRecursively()
            deleteBackUpBucket()
        }
    }

    @Test
    fun testCopyLargeObjectBetweenBuckets() = runTest {
        val testLargeFile = RandomTempFile(10000000)
        val largeFileKey = "largefile"
        val sourceUri = S3Uri(testBucket, largeFileKey)
        try {
            val uploadOperation = s3TransferManager.upload(testLargeFile.path, sourceUri)
            uploadOperation.await()
        } catch (e: S3Exception) {
            testLargeFile.deleteRecursively()
        }

        createBackUpBucket()
        val destUri = S3Uri(backUpBucket, "folder/$largeFileKey")
        try {
            val progressListener = TestingProgressListener()
            val copyOperation = s3TransferManager.copy(sourceUri, destUri, progressListener)
            assertNotNull(copyOperation)
            copyOperation.await()
            checkTransfer(testLargeFile, destUri, copyOperation.progress!!, 1L, 10000000L, 2L)
        } finally {
            testLargeFile.deleteRecursively()
            deleteBackUpBucket()
        }
    }

    @Test
    fun testCopyInvalidFromBucket() = runTest {
        assertFailsWith<IllegalArgumentException>("The copy is completed without throwing source bucket error") {
            coroutineScope {
                val randomSuffux = Random.nextLong(Long.MAX_VALUE)
                val badFromUri = S3Uri("$testBucket$randomSuffux", "key")
                s3TransferManager.copy(badFromUri, S3Uri(backUpBucket, "key")).await()
            }
        }
    }

    @Test
    fun testCopyInvalidFromKey() = runTest {
        assertFailsWith<IllegalArgumentException>("The copy is completed without throwing source key error") {
            coroutineScope {
                val randomKey = "${Random.nextLong(Long.MAX_VALUE)}/${Random.nextInt(Int.MAX_VALUE)}"
                val badFromUri = S3Uri(testBucket, randomKey)
                s3TransferManager.copy(badFromUri, S3Uri(backUpBucket, "key")).await()
            }
        }
    }

    @Test
    fun testCopyInvalidToBucket() = runTest {
        createUploadDirectory()
        val sourceUri = S3Uri(testBucket, "folder1")
        try {
            val operation = s3TransferManager.upload(testUploadDirectory.toString(), sourceUri)
            operation.await()
        } finally {
            testUploadDirectory.toFile().deleteRecursively()
        }

        assertFailsWith<IllegalArgumentException>("The copy is completed without throwing destination bucket error") {
            coroutineScope {
                s3TransferManager.copy(sourceUri, S3Uri(backUpBucket, "folder1")).await()
            }
        }
    }

    @OptIn(InternalSdkApi::class)
    private suspend fun chunksCompare(localFile: File, s3Uri: S3Uri): Boolean {
        val headObjectResponse = s3TransferManager.config.s3.headObjectOrNull(s3Uri)
        if (headObjectResponse == null || !localFile.isFile) {
            return false
        }
        val fileSize = localFile.length()
        if (fileSize != headObjectResponse.contentLength) {
            return false
        }

        val chunkRanges = partition(fileSize, s3TransferManager.config.chunkSize)
        var isEqual = true

        chunkRanges.forEach {
            val contentRange = "bytes=${it.first}-${it.last}"
            val request = GetObjectRequest {
                bucket = s3Uri.bucket
                key = s3Uri.key
                range = contentRange
            }
            s3TransferManager.config.s3.getObject(request) { resp ->
                // compare s3 object chunk and local file's corresponding chunk after converting both to ByteArray
                if (!resp.body?.toByteArray().contentEquals(localFile.asByteStream(it).toByteArray())) {
                    isEqual = false
                    return@getObject
                }
            }
            if (!isEqual) {
                return isEqual
            }
        }

        return isEqual
    }

    /**
     * inner implementation of ProgressListener interface
     */
    private class TestingProgressListener : ProgressListener {
        private var prevProgress: Progress = Progress()

        private val mutex = Mutex()

        override fun onProgress(progress: Progress) = runBlocking {
            mutex.withLock {
                println(progress)
                assertTrue(progress.filesTransferred >= prevProgress.filesTransferred)
                assertTrue(progress.bytesTransferred >= prevProgress.bytesTransferred)
                assertTrue(progress.chunksTransferred >= prevProgress.chunksTransferred)
                prevProgress = progress
            }
        }
    }

    private fun Progress.checkProgress(totalFiles: Long, totalBytes: Long, totalChunks: Long) {
        assertTrue(isDone)
        assertEquals(totalFiles, filesTransferred)
        assertEquals(totalBytes, bytesTransferred)
        assertEquals(totalChunks, chunksTransferred)
    }
}
