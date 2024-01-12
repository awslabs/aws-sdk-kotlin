package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.runtime.auth.credentials.ProcessCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.auth.awssigning.UnsupportedSigningAlgorithm
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class MultRegionAccessPointTest {
    @OptIn(InternalApi::class)
    @Test
    fun testUnsupportedSigningAlgorithm() = runTest {
        val client = S3Client {
            region = "us-east-1"
            credentialsProvider = ProcessCredentialsProvider("isengardcli credentials --awscli aoperez@amazon.com --role Admin")
        }


        val exception = assertThrows<UnsupportedSigningAlgorithm> {
            client.putObject(
                PutObjectRequest {
                    bucket = "..."
                    key = "thisIsATestForMrap"
                }
            )
        }

        assertEquals(
            "SIGV4_ASYMMETRIC support is not yet implemented for the default signer. Please follow the AWS SDK for Kotlin developer guide to set it up with the CRT signer. **LINK TO GUIDE**",
            exception.message
        )
    }

    @Test
    fun testMultiRegionAccessPointOperation() = runTest {
        // TODO
    }
}