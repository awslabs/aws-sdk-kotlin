import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.clientconfig.*
import aws.sdk.kotlin.test.clientconfig.model.ChecksumAlgorithm
import aws.sdk.kotlin.test.clientconfig.model.ValidationMode
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.config.HttpChecksumConfigOption
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.runBlocking
import utils.HeaderReader
import utils.HeaderSetter
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the `aws.protocols#httpChecksum` trait's `requestChecksumRequired` when set to **true**.
 */
class RequestChecksumRequired {
    @Test
    fun requestChecksumRequiredRequestChecksumCalculationWhenSupported(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
        )

        ClientConfigTestClient {
            requestChecksumCalculation = HttpChecksumConfigOption.WHEN_SUPPORTED
            interceptors = mutableListOf(headerReader)
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

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }

    @Test
    fun requestChecksumRequiredRequestChecksumCalculationWhenRequired(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
        )

        ClientConfigTestClient {
            requestChecksumCalculation = HttpChecksumConfigOption.WHEN_REQUIRED
            interceptors = mutableListOf(headerReader)
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

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }
}

/**
 * Tests the `aws.protocols#httpChecksum` trait's `requestChecksumRequired` when set to **false**.
 */
class RequestChecksumNotRequired {
    @Test
    fun requestChecksumNotRequiredRequestChecksumCalculationWhenSupported(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
        )

        ClientConfigTestClient {
            requestChecksumCalculation = HttpChecksumConfigOption.WHEN_SUPPORTED
            interceptors = mutableListOf(headerReader)
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

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }

    @Test
    fun requestChecksumNotRequiredRequestChecksumCalculationWhenRequired(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
        )

        ClientConfigTestClient {
            requestChecksumCalculation = HttpChecksumConfigOption.WHEN_REQUIRED
            interceptors = mutableListOf(headerReader)
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

        assertFalse(
            headerReader.containsExpectedHeaders,
        )
    }
}

/**
 * Tests the `aws.protocols#httpChecksum` trait's `requestAlgorithmMember`.
 */
class UserSelectedChecksumAlgorithm {
    @Test
    fun userSelectedChecksumAlgorithmIsUsed(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-sha256" to null),
        )

        ClientConfigTestClient {
            interceptors = mutableListOf(headerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.checksumsRequiredOperation {
                body = "Hello World!"
                checksumAlgorithm = ChecksumAlgorithm.Sha256
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }
}

/**
 * Tests user provided checksum calculations.
 */
class UserProvidedChecksumHeader {
    @Test
    fun userProvidedChecksumIsUsed(): Unit = runBlocking {
        val headerSetter = HeaderSetter(
            mapOf("x-amz-checksum-crc64nvme" to "foo"),
        )
        val headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc64nvme" to "foo"),
            forbiddenHeaders = mapOf("x-amz-checksum-sha256" to "foo"),
        )

        ClientConfigTestClient {
            interceptors = mutableListOf(headerSetter, headerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.checksumsRequiredOperation {
                body = "Hello World!"
                checksumAlgorithm = ChecksumAlgorithm.Sha256
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders,
        )

        assertFalse(
            headerReader.containsForbiddenHeaders,
        )
    }

    @Test
    fun unmodeledChecksumIsUsed(): Unit = runBlocking {
        val headerSetter = HeaderSetter(
            mapOf("x-amz-checksum-some-future-algorithm" to "foo"),
        )
        val headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-some-future-algorithm" to "foo"),
            forbiddenHeaders = mapOf(
                "x-amz-checksum-crc32" to "foo",
                "x-amz-checksum-sha256" to "foo",
            ),
        )

        ClientConfigTestClient {
            interceptors = mutableListOf(headerSetter, headerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.checksumsRequiredOperation {
                body = "Hello World!"
                checksumAlgorithm = ChecksumAlgorithm.Sha256
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }

    @Test
    fun userProvidedMd5IsNotUsed(): Unit = runBlocking {
        val headerSetter = HeaderSetter(
            mapOf("x-amz-checksum-md5" to "foo"),
        )
        val headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
            forbiddenHeaders = mapOf(
                "x-amz-checksum-md5" to "foo",
            ),
        )

        ClientConfigTestClient {
            interceptors = mutableListOf(headerSetter, headerReader)
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

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }
}

/**
 * Tests the `aws.protocols#httpChecksum` trait's `requestValidationModeMember`.
 */
class ResponseChecksumValidation {
    @Test
    fun responseChecksumValidationWhenSupported(): Unit = runBlocking {
        assertFailsWith<ChecksumMismatchException> {
            ClientConfigTestClient {
                responseChecksumValidation = HttpChecksumConfigOption.WHEN_SUPPORTED
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-crc32", "I will trigger `ChecksumMismatchException` if read!")
                            },
                            "World!".toHttpBody(),
                        )
                        val now = Instant.now()
                        HttpCall(request, resp, now, now)
                    },
                )
                credentialsProvider = StaticCredentialsProvider(
                    Credentials("accessKeyID", "secretAccessKey"),
                )
                region = "us-east-1"
            }.use { client ->
                client.checksumsRequiredOperation {
                    body = "Hello"
                }
            }
        }
    }

    @Test
    fun responseChecksumValidationWhenRequired(): Unit = runBlocking {
        ClientConfigTestClient {
            responseChecksumValidation = HttpChecksumConfigOption.WHEN_REQUIRED
            httpClient = TestEngine(
                roundTripImpl = { _, request ->
                    val resp = HttpResponse(
                        HttpStatusCode.OK,
                        Headers {
                            append("x-amz-checksum-crc32", "I will trigger `ChecksumMismatchException` if read!")
                        },
                        "World!".toHttpBody(),
                    )
                    val now = Instant.now()
                    HttpCall(request, resp, now, now)
                },
            )
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.checksumsRequiredOperation {
                body = "Hello"
            }
        }
    }

    @Test
    fun responseChecksumValidationWhenRequiredWithRequestValidationModeEnabled(): Unit = runBlocking {
        assertFailsWith<ChecksumMismatchException> {
            ClientConfigTestClient {
                responseChecksumValidation = HttpChecksumConfigOption.WHEN_REQUIRED
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-crc32", "I will trigger `ChecksumMismatchException` if read!")
                            },
                            "World!".toHttpBody(),
                        )
                        val now = Instant.now()
                        HttpCall(request, resp, now, now)
                    },
                )
                credentialsProvider = StaticCredentialsProvider(
                    Credentials("accessKeyID", "secretAccessKey"),
                )
                region = "us-east-1"
            }.use { client ->
                client.checksumsRequiredOperation {
                    body = "Hello"
                    validationMode = ValidationMode.Enabled
                }
            }
        }
    }

    @Test
    fun compositeChecksumsAreNotValidated(): Unit = runBlocking {
        ClientConfigTestClient {
            responseChecksumValidation = HttpChecksumConfigOption.WHEN_REQUIRED
            httpClient = TestEngine(
                roundTripImpl = { _, request ->
                    val resp = HttpResponse(
                        HttpStatusCode.OK,
                        Headers {
                            append(
                                "x-amz-checksum-crc32",
                                "I'm a composite checksum and will trigger `ChecksumMismatchException` if read!-1",
                            )
                        },
                        "World!".toHttpBody(),
                    )
                    val now = Instant.now()
                    HttpCall(request, resp, now, now)
                },
            )
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.checksumsRequiredOperation {
                body = "Hello"
            }
        }
    }
}
