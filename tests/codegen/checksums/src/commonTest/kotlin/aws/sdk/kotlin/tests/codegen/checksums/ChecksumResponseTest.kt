/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.checksums

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.checksums.*
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.io.source
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

private val responseBody = "Hello world"

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
                        object : HttpBody.SourceContent() {
                            override val isOneShot: Boolean = false
                            override val contentLength: Long? = responseBody.length.toLong()
                            override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                        },
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
                        object : HttpBody.SourceContent() {
                            override val isOneShot: Boolean = false
                            override val contentLength: Long? = responseBody.length.toLong()
                            override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                        },
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
                        object : HttpBody.SourceContent() {
                            override val isOneShot: Boolean = false
                            override val contentLength: Long? = responseBody.length.toLong()
                            override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                        },
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
                        object : HttpBody.SourceContent() {
                            override val isOneShot: Boolean = false
                            override val contentLength: Long? = responseBody.length.toLong()
                            override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                        },
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
                            object : HttpBody.SourceContent() {
                                override val isOneShot: Boolean = false
                                override val contentLength: Long? = responseBody.length.toLong()
                                override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                            },
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
                            object : HttpBody.SourceContent() {
                                override val isOneShot: Boolean = false
                                override val contentLength: Long? = responseBody.length.toLong()
                                override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                            },
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
                            object : HttpBody.SourceContent() {
                                override val isOneShot: Boolean = false
                                override val contentLength: Long? = responseBody.length.toLong()
                                override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                            },
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
                            object : HttpBody.SourceContent() {
                                override val isOneShot: Boolean = false
                                override val contentLength: Long? = responseBody.length.toLong()
                                override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                            },
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