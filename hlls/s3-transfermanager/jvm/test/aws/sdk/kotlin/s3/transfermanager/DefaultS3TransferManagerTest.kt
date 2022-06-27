
import aws.sdk.kotlin.s3.transfermanager.DefaultS3TransferManager
import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultS3TransferManagerTest {

    private val fromList = listOf(
        "/Users/wty/Desktop/folder1/a.png",
        "/Users/wty/Desktop/folder1/",
        "/Users/wty/Desktop/folder1"
    )
    private val toList = listOf(
        S3Uri("wty-bucket", "file1"),
        S3Uri("wty-bucket", "folder1"),
        S3Uri("S3://wty-bucket/folder2/folder3")
    )

    @Test
    fun testUpload() = runTest {
        val s3TranferManager = DefaultS3TransferManager()
        for (i in 0 .. fromList.size - 1) {     // test uploading a single file or directory with different suffix
            val operation = s3TranferManager.upload(fromList[i], toList[i])
            assertNotNull(operation, "The transfer manager doesn't tackle upload")
        }
    }
}