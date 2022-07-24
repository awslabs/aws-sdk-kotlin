package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.s3.transfermanager.S3TransferManager
import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.headObject
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

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

    private lateinit var testUploadDirectory: Path

    private lateinit var testDownloadDirectory: Path

    @BeforeAll
    private fun createResources(): Unit = runBlocking {
        val home: String = System.getProperty("user.home")
        val dir = Paths.get(home, "Downloads")
        testBucket = S3TransferManagerTestUtils.getTestBucket(s3TransferManager.config.s3)
        testUploadDirectory = Files.createTempDirectory(dir, "testUploadDirectory")
        testDownloadDirectory = Files.createTempDirectory(dir, "testDownloadDirectory")

        val file1 = File.createTempFile("file1", ".txt", testUploadDirectory.toFile())
        val testUploadDirectory1 = Files.createTempDirectory(testUploadDirectory, "testUploadDirectory1")
        val file2 = File.createTempFile("file2", ".png", testUploadDirectory1.toFile())
        val file3 = File.createTempFile("file3", ".jpeg", testUploadDirectory1.toFile())
        val testUploadDirectory2 = Files.createTempDirectory(testUploadDirectory, "testUploadDirectory2")

        Runtime.getRuntime().addShutdownHook(
            Thread {
                file1.delete()
                file2.delete()
                file3.delete()
                testUploadDirectory1.toFile().delete()
                testUploadDirectory2.toFile().delete()
                testUploadDirectory.toFile().delete()
            }
        )
    }

    @AfterAll
    private fun cleanup() = runBlocking {
        S3TransferManagerTestUtils.deleteBucketAndAllContents(s3TransferManager.config.s3, testBucket)
    }

    @Test
    fun testUpload() = runTest {
        val keyPrefix = "folder1"
        val toUri = S3Uri(testBucket, keyPrefix)

        var operation = s3TransferManager.upload(testUploadDirectory.toString(), toUri)
        assertNotNull(operation, "The transfer manager didn't start directory upload")
        operation.await()
        val listObjectsResponse = s3TransferManager.config.s3.listObjectsV2 {
            bucket = testBucket
            prefix = keyPrefix
        }
        assert(listObjectsResponse.contents!!.isNotEmpty())

        val testLargeFile = RandomTempFile(10000000)
        operation = s3TransferManager.upload(testLargeFile.path, toUri)
        assertNotNull(operation, "The transfer manager didn't start parts upload")
        operation.await()
        val headObjectResponse = s3TransferManager.config.s3.headObject {
            bucket = testBucket
            key = keyPrefix
        }
        assertNotNull(headObjectResponse)
    }

    @Test
    fun testUploadInvalidFrom() = runTest {
        assertFailsWith<IllegalArgumentException>("The upload is completed without throwing invalid from path error") {
            coroutineScope {
                s3TransferManager.upload("/Users/blabla/Desk/haha", S3Uri("s3://wty-bucket/key")).await()
            }
        }
    }

    @Test
    fun testDownload() = runTest {
        val s3Uri = S3Uri(testBucket, "folder1")
        var operation = s3TransferManager.upload(testUploadDirectory.toString(), s3Uri)
        operation.await()

        operation = s3TransferManager.download(s3Uri, testDownloadDirectory.toString())
        assertNotNull(operation, "The transfer manager didn't start directory download")
        operation.await()
        val dirStream = Files.newDirectoryStream(testDownloadDirectory)
        assert(dirStream.iterator().hasNext())
    }

    @Test
    fun testDownloadInvalidFromBucket() = runTest {
        val s3Uri = S3Uri(testBucket, "folder1")
        var operation = s3TransferManager.upload(testUploadDirectory.toString(), s3Uri)
        operation.await()

        assertFailsWith<IllegalArgumentException>("The download is completed without throwing from bucket error") {
            coroutineScope {
                s3TransferManager.download(S3Uri("s3://${testBucket}14y127864/folder1"), "/Users/wty/Desktop/folder1/haha").await()
            }
        }
    }

    @Test
    fun testDownloadInvalidFromKey() = runTest {
        val s3Uri = S3Uri(testBucket, "folder1")
        var operation = s3TransferManager.upload(testUploadDirectory.toString(), s3Uri)
        operation.await()

        assertFailsWith<IllegalArgumentException>("The download is completed without throwing from key error") {
            coroutineScope {
                s3TransferManager.download(S3Uri(testBucket, "haha/"), "/Users/wty/Desktop/folder1/haha").await()
            }
        }
    }
}
