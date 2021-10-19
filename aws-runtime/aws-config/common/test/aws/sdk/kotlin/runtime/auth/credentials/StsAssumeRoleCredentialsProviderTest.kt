package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ACCESS_KEY = "ACCESS_KEY"
private const val SECRET_ACCESS_KEY = "SECRET_ACCESS_KEY"
private const val SESSION_TOKEN = "SESSION_TOKEN"

class StsAssumeRoleCredentialsProviderTest {

    @Test
    fun itConvertsSdkCredentialProviderToCrtType() = runSuspendTest {
        val sdkCP = object : CredentialsProvider {
            override suspend fun getCredentials(): Credentials {
                return Credentials(ACCESS_KEY, SECRET_ACCESS_KEY, SESSION_TOKEN)
            }
        }

        val crtCP = adapt(sdkCP)
        val cpCredentials = crtCP.getCredentials()

        assertEquals(ACCESS_KEY, cpCredentials.accessKeyId)
        assertEquals(SECRET_ACCESS_KEY, cpCredentials.secretAccessKey)
        assertEquals(SESSION_TOKEN, cpCredentials.sessionToken)
    }
}
