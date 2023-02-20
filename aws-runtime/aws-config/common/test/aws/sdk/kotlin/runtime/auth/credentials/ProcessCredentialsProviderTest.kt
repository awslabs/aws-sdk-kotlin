package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.time.Instant
import io.mockk.coEvery
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessCredentialsProviderTest {
    @Test
    fun testSuccessWithExpiringCredentials() = runTest {
        mockkStatic(::executeCommand)
        coEvery { executeCommand(any(), any(), any(), any(), any()) }.returns(
            Pair(
                0,
                """
            {
                "Version": 1,
                "AccessKeyId": "AccessKeyId",
                "SecretAccessKey": "SecretAccessKey",
                "SessionToken": "SessionToken",
                "Expiration": "2022-10-14T00:00:00Z"
            }
                """.trimIndent(),
            ),
        )

        val expectedCredentials = Credentials(
            accessKeyId = "AccessKeyId",
            secretAccessKey = "SecretAccessKey",
            sessionToken = "SessionToken",
            expiration = Instant.fromEpochSeconds(1665705600),
            providerName = "Process",
        )

        val processCredentialsProvider = ProcessCredentialsProvider("anyString")
        val actualCredentials = processCredentialsProvider.getCredentials()
        assertEquals(expectedCredentials, actualCredentials)
    }

    @Test
    fun testSuccessWithNonExpiringCredentials() = runTest {
        mockkStatic(::executeCommand)
        coEvery { executeCommand(any(), any(), any(), any(), any()) }.returns(
            Pair(
                0,
                """
            {
                "Version": 1,
                "AccessKeyId": "AccessKeyId",
                "SecretAccessKey": "SecretAccessKey",
                "SessionToken": "SessionToken"
            }
                """.trimIndent(),
            ),
        )

        val expectedCredentials = Credentials(
            accessKeyId = "AccessKeyId",
            secretAccessKey = "SecretAccessKey",
            sessionToken = "SessionToken",
            expiration = Instant.MAX_VALUE,
            providerName = "Process",
        )

        val processCredentialsProvider = ProcessCredentialsProvider("anyString")
        val actualCredentials = processCredentialsProvider.getCredentials()
        assertEquals(expectedCredentials, actualCredentials)
    }

    @Test
    fun testMissingVersion() = runTest {
        mockkStatic(::executeCommand)
        coEvery { executeCommand(any(), any(), any(), any(), any()) }.returns(
            Pair(
                0,
                """
            {
                "AccessKeyId": "AccessKeyId",
                "SecretAccessKey": "SecretAccessKey",
                "SessionToken": "SessionToken",
                "Expiration": "2022-10-14T00:00:00Z"
            }
                """.trimIndent(),
            ),
        )

        val processCredentialsProvider = ProcessCredentialsProvider("anyString")
        assertFailsWith<InvalidJsonCredentialsException> {
            processCredentialsProvider.getCredentials()
        }
    }

    @Test
    fun testUnsupportedVersion() = runTest {
        mockkStatic(::executeCommand)
        coEvery { executeCommand(any(), any(), any(), any(), any()) }.returns(
            Pair(
                0,
                """
            {
                "Version": 2
                "AccessKeyId": "AccessKeyId",
                "SecretAccessKey": "SecretAccessKey",
                "SessionToken": "SessionToken",
                "Expiration": "2022-10-14T00:00:00Z"
            }
                """.trimIndent(),
            ),
        )

        val processCredentialsProvider = ProcessCredentialsProvider("anyString")
        assertFailsWith<InvalidJsonCredentialsException> {
            processCredentialsProvider.getCredentials()
        }
    }

    @Test
    fun testCommandFailure() = runTest {
        val exitCode = 1 // nonzero exit code indicates failure
        val stderr = "stderr message!"

        mockkStatic(::executeCommand)
        coEvery { executeCommand(any(), any(), any(), any(), any()) }.returns(Pair(exitCode, stderr))

        val processCredentialsProvider = ProcessCredentialsProvider("anyString")
        val ex = assertFailsWith<CredentialsProviderException> { processCredentialsProvider.getCredentials() }
        assertContains(ex.message!!, stderr) // the exception message should contain the program's stderr
    }
}
