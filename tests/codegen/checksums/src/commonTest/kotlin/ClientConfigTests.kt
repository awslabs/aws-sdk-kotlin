import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.clientconfig.*
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.client.config.RequestChecksumCalculation
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import aws.smithy.kotlin.runtime.httptest.TestEngine

class ClientConfigTests {
    @Test
    fun requestChecksumCalculationWhenSupported(): Unit = runBlocking {
        val testInterceptor = TestInterceptor()

        ClientConfigTestClient {
            requestChecksumCalculation = RequestChecksumCalculation.WHEN_SUPPORTED
            interceptors = mutableListOf(testInterceptor)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.checksumsNotRequiredOperation {
                body = "Hello World!"
            }
        }

        assertTrue(testInterceptor.containsChecksum)
    }

    @Test
    fun requestChecksumCalculationWhenRequired(): Unit = runBlocking {
        val testInterceptor = TestInterceptor()

        ClientConfigTestClient {
            requestChecksumCalculation = RequestChecksumCalculation.WHEN_REQUIRED
            interceptors = mutableListOf(testInterceptor)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.checksumsNotRequiredOperation {
                body = "Hello World!"
            }
        }

        assertFalse(testInterceptor.containsChecksum)
    }
}

private class TestInterceptor : HttpInterceptor {
    var containsChecksum = false

    override suspend fun modifyBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        containsChecksum = context.protocolRequest.headers.contains("x-amz-checksum-crc32")
        return context.protocolRequest
    }
}
