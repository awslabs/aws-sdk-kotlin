import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.checksums.*
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.toHttpBody
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SuccessfulChecksumResponseTest {
    @Test
    fun crc32(): Unit = runBlocking {
        TestClient {
            httpClient = TestEngine(
                roundTripImpl = { _, request ->
                    val resp = HttpResponse(
                        HttpStatusCode.OK,
                        Headers {
                            append("x-amz-checksum-crc32", "i9aeUg==")
                        },
                        "Hello world".toHttpBody(),
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
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
            }
        }
    }

    @Test
    fun crc32c(): Unit = runBlocking {
        TestClient {
            httpClient = TestEngine(
                roundTripImpl = { _, request ->
                    val resp = HttpResponse(
                        HttpStatusCode.OK,
                        Headers {
                            append("x-amz-checksum-crc32c", "crUfeA==")
                        },
                        "Hello world".toHttpBody(),
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
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
            }
        }
    }

    @Test
    fun sha1(): Unit = runBlocking {
        TestClient {
            httpClient = TestEngine(
                roundTripImpl = { _, request ->
                    val resp = HttpResponse(
                        HttpStatusCode.OK,
                        Headers {
                            append("x-amz-checksum-sha1", "e1AsOh9IyGCa4hLN+2Od7jlnP14=")
                        },
                        "Hello world".toHttpBody(),
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
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
            }
        }
    }

    @Test
    fun sha256(): Unit = runBlocking {
        TestClient {
            httpClient = TestEngine(
                roundTripImpl = { _, request ->
                    val resp = HttpResponse(
                        HttpStatusCode.OK,
                        Headers {
                            append("x-amz-checksum-sha256", "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=")
                        },
                        "Hello world".toHttpBody(),
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
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
            }
        }
    }
}

class FailedChecksumResponseTest {
    @Test
    fun crc32(): Unit = runBlocking {
        assertFailsWith<ChecksumMismatchException> {
            TestClient {
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-crc32", "bm90LWEtY2hlY2tzdW0=")
                            },
                            "Hello world".toHttpBody(),
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
                client.httpChecksumOperation {
                    body = "Hello world".encodeToByteArray()
                }
            }
        }
    }

    @Test
    fun crc32c(): Unit = runBlocking {
        assertFailsWith<ChecksumMismatchException> {
            TestClient {
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-crc32c", "bm90LWEtY2hlY2tzdW0=")
                            },
                            "Hello world".toHttpBody(),
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
                client.httpChecksumOperation {
                    body = "Hello world".encodeToByteArray()
                }
            }
        }
    }

    @Test
    fun sha1(): Unit = runBlocking {
        assertFailsWith<ChecksumMismatchException> {
            TestClient {
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-sha1", "bm90LWEtY2hlY2tzdW0=")
                            },
                            "Hello world".toHttpBody(),
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
                client.httpChecksumOperation {
                    body = "Hello world".encodeToByteArray()
                }
            }
        }
    }

    @Test
    fun sha256(): Unit = runBlocking {
        assertFailsWith<ChecksumMismatchException> {
            TestClient {
                httpClient = TestEngine(
                    roundTripImpl = { _, request ->
                        val resp = HttpResponse(
                            HttpStatusCode.OK,
                            Headers {
                                append("x-amz-checksum-sha256", "bm90LWEtY2hlY2tzdW0=")
                            },
                            "Hello world".toHttpBody(),
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
                client.httpChecksumOperation {
                    body = "Hello world".encodeToByteArray()
                }
            }
        }
    }
}
