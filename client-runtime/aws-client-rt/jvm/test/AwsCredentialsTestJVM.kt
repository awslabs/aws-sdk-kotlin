import org.junit.Test
import software.aws.kotlinsdk.ProfileAwsCredentialsProvider
import kotlin.test.assertNotNull

class AwsCredentialsTestJVM {

    // TODO: update this test such that a file on the rootfs is not required
    @Test
    fun `can read credentials file on JVM`() {
        val unit = ProfileAwsCredentialsProvider()
        val credentials = unit.invoke()

        assertNotNull(credentials)
        assertNotNull(credentials.accessKey)
        assertNotNull(credentials.secretKey)
    }
}