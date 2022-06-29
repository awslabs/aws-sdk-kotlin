
import aws.sdk.kotlin.s3.transfermanager.DefaultS3TransferManager
import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultS3TransferManagerTest {

    val s3TranferManager = DefaultS3TransferManager()

    private val fromList = listOf(
        "/Users/wty/Desktop/folder1/a.png",
        "/Users/wty/Desktop/folder1/",
        "/Users/wty/Desktop/folder1",
        "/Users/wty/Downloads/largeimage.png",
    )
    private val toList = listOf(
        S3Uri("wty-bucket", "file1"),
        S3Uri("wty-bucket", "folder1"),
        S3Uri("S3://wty-bucket/folder2/folder3"),
        S3Uri("S3://wty-bucket/large-folder/to_here/190MB")
    )

    @Test
    fun testUpload() = runTest {
        for (i in 0 until fromList.size) {     // test uploading a single file or directory with different suffix
            val operation = s3TranferManager.upload(fromList[i], toList[i])
            assertNotNull(operation, "The transfer manager doesn't tackle upload")
        }
    }

    @Test
    fun testUploadInvalidFrom() = runTest {
        try {
            s3TranferManager.upload("/Users/wty/Desktop/folder1/haha", S3Uri("S3://wty-bucket/key"))
        } catch (e: IllegalArgumentException) {
            assertEquals(e.message, "From path is invalid")
        }
    }
}