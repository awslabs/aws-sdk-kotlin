import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.clientconfig.*
import aws.sdk.kotlin.test.clientconfig.model.ValidationMode
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.client.config.ChecksumConfigOption
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientConfigTests {
    @Nested
    inner class RequestChecksumNotRequired {
        @Test
        @Ignore // todo: un-ignore
        fun requestChecksumNotRequiredRequestChecksumCalculationWhenSupported(): Unit = runBlocking {
            val requestInterceptor = RequestInterceptor()

            ClientConfigTestClient {
                requestChecksumCalculation = ChecksumConfigOption.WHEN_SUPPORTED
                interceptors = mutableListOf(requestInterceptor)
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

            assertTrue(requestInterceptor.containsChecksum)
        }

        @Test
        fun requestChecksumNotRequiredRequestChecksumCalculationWhenRequired(): Unit = runBlocking {
            val requestInterceptor = RequestInterceptor()

            ClientConfigTestClient {
                requestChecksumCalculation = ChecksumConfigOption.WHEN_REQUIRED
                interceptors = mutableListOf(requestInterceptor)
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

            assertFalse(requestInterceptor.containsChecksum)
        }
    }

    @Nested
    inner class RequestChecksumRequired {
        @Test
        @Ignore // todo: un-ignore
        fun requestChecksumRequiredRequestChecksumCalculationWhenSupported(): Unit = runBlocking {
            val requestInterceptor = RequestInterceptor()

            ClientConfigTestClient {
                requestChecksumCalculation = ChecksumConfigOption.WHEN_SUPPORTED
                interceptors = mutableListOf(requestInterceptor)
                httpClient = TestEngine()
                credentialsProvider = StaticCredentialsProvider(
                    Credentials("accessKeyID", "secretAccessKey"),
                )
                region = "us-east-1"
            }.use { client ->
                client.checksumsRequiredOperation {
                    body = "Hello World!"
                }
            }

            assertTrue(requestInterceptor.containsChecksum)
        }

        @Test
        @Ignore // todo: un-ignore
        fun requestChecksumRequiredRequestChecksumCalculationWhenRequired(): Unit = runBlocking {
            val requestInterceptor = RequestInterceptor()

            ClientConfigTestClient {
                requestChecksumCalculation = ChecksumConfigOption.WHEN_REQUIRED
                interceptors = mutableListOf(requestInterceptor)
                httpClient = TestEngine()
                credentialsProvider = StaticCredentialsProvider(
                    Credentials("accessKeyID", "secretAccessKey"),
                )
                region = "us-east-1"
            }.use { client ->
                client.checksumsRequiredOperation {
                    body = "Hello World!"
                }
            }

            assertTrue(requestInterceptor.containsChecksum)
        }
    }

    @Nested
    inner class ResponseChecksumValidation {
        @Test
        @Ignore // todo - unignore
        fun responseChecksumValidationResponseChecksumValidationWhenSupported(): Unit = runBlocking {
            var responseChecksumValidated = false

            ClientConfigTestClient {
                responseChecksumValidation = ChecksumConfigOption.WHEN_SUPPORTED
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-crc32", "bogus")
                            },
                            "Goodbye!".toHttpBody(),
                        )
                        val now = Instant.now()
                        HttpCall(request, resp, now, now)
                    }
                )
                credentialsProvider = StaticCredentialsProvider(
                    Credentials("accessKeyID", "secretAccessKey"),
                )
                region = "us-east-1"
            }.use { client ->
                try {
                    client.checksumsRequiredOperation {
                        body = "Hello World!"
                    }
                } catch (_: ChecksumMismatchException) { // "bogus" is not a matching checksum
                    responseChecksumValidated = true
                }
            }

            assertTrue(responseChecksumValidated)
        }

        @Test
        fun responseChecksumValidationResponseChecksumValidationWhenRequired(): Unit = runBlocking {
            var responseChecksumValidated = false

            ClientConfigTestClient {
                responseChecksumValidation = ChecksumConfigOption.WHEN_REQUIRED
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-crc32", "bogus")
                            },
                            "Goodbye!".toHttpBody(),
                        )
                        val now = Instant.now()
                        HttpCall(request, resp, now, now)
                    }
                )
                credentialsProvider = StaticCredentialsProvider(
                    Credentials("accessKeyID", "secretAccessKey"),
                )
                region = "us-east-1"
            }.use { client ->
                try {
                    client.checksumsRequiredOperation {
                        body = "Hello World!"
                    }
                } catch (_: ChecksumMismatchException) { // "bogus" is not a matching checksum
                    responseChecksumValidated = true
                }
            }

            assertFalse(responseChecksumValidated)
        }

        @Test
        @Ignore // todo - unignore
        fun responseChecksumValidationResponseChecksumValidationWhenRequiredWithRequestValidationModeMember(): Unit = runBlocking {
            var responseChecksumValidated = false

            ClientConfigTestClient {
                responseChecksumValidation = ChecksumConfigOption.WHEN_REQUIRED
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-crc32", "bogus")
                            },
                            "Goodbye!".toHttpBody(),
                        )
                        val now = Instant.now()
                        HttpCall(request, resp, now, now)
                    }
                )
                credentialsProvider = StaticCredentialsProvider(
                    Credentials("accessKeyID", "secretAccessKey"),
                )
                region = "us-east-1"
            }.use { client ->
                try {
                    client.checksumsRequiredOperation {
                        body = "Hello World!"
                    }
                } catch (_: ChecksumMismatchException) { // "bogus" is not a matching checksum
                    responseChecksumValidated = true
                }
            }

            assertTrue(responseChecksumValidated)
        }
    }
}

private class RequestInterceptor : HttpInterceptor {
    var containsChecksum = false

    override suspend fun modifyBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        containsChecksum = context.protocolRequest.headers.contains("x-amz-checksum-crc32") // default checksum algorithm
        return context.protocolRequest
    }
}
