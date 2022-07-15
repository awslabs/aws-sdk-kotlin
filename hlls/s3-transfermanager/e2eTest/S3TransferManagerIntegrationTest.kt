package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.s3.transfermanager.S3TransferManager
import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private lateinit var testDirectory: Path

    @BeforeAll
    private fun createResources(): Unit = runBlocking {
        val home: String = System.getProperty("user.home")
        val dir = Paths.get(home, "Downloads")
        testBucket = S3TransferManagerTestUtils.getTestBucket(s3TransferManager.config.s3)
        testDirectory = Files.createTempDirectory(dir, "TestDirectory")

        val file1 = File.createTempFile("file1", ".txt", testDirectory.toFile())
        val testDirectory1 = Files.createTempDirectory(testDirectory, "TestDirectory1")
        val file2 = File.createTempFile("file2", ".png", testDirectory1.toFile())
        val file3 = File.createTempFile("file3", ".jpeg", testDirectory1.toFile())
        val testDirectory2 = Files.createTempDirectory(testDirectory, "TestDirectory2")

        Runtime.getRuntime().addShutdownHook(
            Thread {
                file1.delete()
                file2.delete()
                file3.delete()
                testDirectory1.toFile().delete()
                testDirectory2.toFile().delete()
                testDirectory.toFile().delete()
            }
        )
    }

    @AfterAll
    private fun cleanup() = runBlocking {
        S3TransferManagerTestUtils.deleteBucketAndAllContents(s3TransferManager.config.s3, testBucket)
    }

    @Test
    fun testUpload() = runTest {
        val toUri = S3Uri(testBucket, "folder1")

        var operation = s3TransferManager.upload(testDirectory.toString(), toUri)
        assertNotNull(operation, "The transfer manager doesn't tackle directory upload")

        val testLargeFile = RandomTempFile(10000000)
        operation = s3TransferManager.upload(testLargeFile.path, toUri)
        assertNotNull(operation, "The transfer manager doesn't tackle parts upload")
    }

    @Test
    fun testUploadInvalidFrom() = runTest {

        assertFailsWith<IllegalArgumentException>("From path is invalid") {
            s3TransferManager.upload("/Users/wty/Desktop/folder1/haha", S3Uri("S3://wty-bucket/key"))
        }
    }

    @Test
    fun testDownload() = runTest {
        val s3Uri = S3Uri(testBucket, "folder1")
        s3TransferManager.upload(testDirectory.toString(), s3Uri)

        val operation = s3TransferManager.download(s3Uri, testDirectory.toString())
        assertNotNull(operation, "The transfer manager doesn't tackle directory download")
    }
}
