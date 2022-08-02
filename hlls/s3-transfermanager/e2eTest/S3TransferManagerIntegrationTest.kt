package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.s3.transfermanager.S3TransferManager
import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.s3.transfermanager.ensureEndsWith
import aws.sdk.kotlin.s3.transfermanager.headObjectOrNull
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.random.Random
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
        Files.createTempFile(testUploadDirectory, "file1", ".txt")
        val testUploadDirectory1 = Files.createTempDirectory(testUploadDirectory, "testUploadDirectory1")
        Files.createTempFile(testUploadDirectory1, "file2", ".png")
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

    private fun File.deleteRecursive() {
        if (isDirectory) {
            listFiles().forEach {
                it.deleteRecursive()
            }
        }
        delete()
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
        File.createTempFile("file1", ".txt", testUploadDirectory.toFile())
        val testUploadDirectory1 = Files.createTempDirectory(testUploadDirectory, "testUploadDirectory1")
        File.createTempFile("file2", ".png", testUploadDirectory1.toFile())
        File.createTempFile("file3", ".jpeg", testUploadDirectory1.toFile())
        Files.createTempDirectory(testUploadDirectory, "testUploadDirectory2")
    }

    private fun createDownloadDirectory() {
        val home: String = System.getProperty("user.home")
        val dir = Paths.get(home, "Downloads")
        testDownloadDirectory = Files.createTempDirectory(dir, "testDownloadDirectory")
    }

    private fun File.deleteRecursive() {
        if (isDirectory) {
            listFiles().forEach {
                it.deleteRecursive()
            }
        }
        delete()
    }

    @Test
    fun testUpload() = runTest {
        createUploadDirectory()
        val keyPrefix = "folder1"
        val toUri = S3Uri(testBucket, keyPrefix)
        try {
            val operation = s3TransferManager.upload(testUploadDirectory.toString(), toUri)
            assertNotNull(operation, "The transfer manager didn't start directory upload")
            operation.await()
            assertTrue(checkUpload(testUploadDirectory.toFile(), toUri))
        } finally {
            testUploadDirectory.toFile().deleteRecursive()
        }
    }

    @Test
    fun testUploadLargeFile() = runTest {
        val testLargeFile = RandomTempFile(10000000)
        val largeFileKey = "largefile"
        val toUri = S3Uri(testBucket, largeFileKey)
        try {
            val operation = s3TransferManager.upload(testLargeFile.path, toUri)
            assertNotNull(operation, "The transfer manager didn't start parts upload")
            operation.await()
            assertTrue(checkUpload(testLargeFile, toUri))
        } finally {
            testLargeFile.deleteRecursive()
        }
    }

    private suspend fun checkUpload(localFile: File, to: S3Uri): Boolean {
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
                    if (!checkUpload(it, subTo)) {
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
            testUploadDirectory.toFile().deleteRecursive()
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
            testUploadDirectory.toFile().deleteRecursive()
        }

        createDownloadDirectory()
        try {
            val downloadOperation = s3TransferManager.download(s3Uri, testDownloadDirectory.toString())
            assertNotNull(downloadOperation, "The transfer manager didn't start directory download")
            downloadOperation.await()
            assertTrue(checkDownload(s3Uri, testDownloadDirectory.toFile()))
        } finally {
            testDownloadDirectory.toFile().deleteRecursive()
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
            testLargeFile.deleteRecursive()
        }

        createDownloadDirectory()
        try {
            val downloadOperation = s3TransferManager.download(s3Uri, testDownloadDirectory.toString())
            assertNotNull(downloadOperation)
            downloadOperation.await()
            val downloadFile = Paths.get(testDownloadDirectory.toString(), largeFileKey).toFile()
            assertTrue(checkDownload(s3Uri, downloadFile))
        } finally {
            testDownloadDirectory.toFile().deleteRecursive()
        }
    }

    @OptIn(InternalSdkApi::class)
    private suspend fun checkDownload(from: S3Uri, localFile: File): Boolean {
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
                if (!checkDownload(subFrom, subFile)) {
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
            val copyOperation = s3TransferManager.copy(sourceUri, destUri)
            copyOperation.await()
            assertNotNull(copyOperation)
            assertTrue(checkUpload(testUploadDirectory.toFile(), destUri))
        } finally {
            testUploadDirectory.toFile().deleteRecursive()
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
            val copyOperation = s3TransferManager.copy(sourceUri, destUri)
            assertNotNull(copyOperation)
            copyOperation.await()
            assertTrue(checkUpload(testLargeFile, destUri))
        } finally {
            testLargeFile.deleteRecursive()
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
            testUploadDirectory.toFile().deleteRecursive()
            throw e
        }

        createBackUpBucket()
        val destUri = S3Uri(backUpBucket, "folder/folder1")
        try {
            val copyOperation = s3TransferManager.copy(sourceUri, destUri)
            copyOperation.await()
            assertNotNull(copyOperation)
            assertTrue(checkUpload(testUploadDirectory.toFile(), destUri))
        } finally {
            testUploadDirectory.toFile().deleteRecursive()
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
            testLargeFile.deleteRecursive()
        }

        createBackUpBucket()
        val destUri = S3Uri(backUpBucket, "folder/$largeFileKey")
        try {
            val copyOperation = s3TransferManager.copy(sourceUri, destUri)
            assertNotNull(copyOperation)
            copyOperation.await()
            assertTrue(checkUpload(testLargeFile, destUri))
        } finally {
            testLargeFile.deleteRecursive()
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
            testUploadDirectory.toFile().deleteRecursive()
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
}
