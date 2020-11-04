/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.aws.kotlinsdk.restjson

import software.aws.clientrt.http.*
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.feature.DeserializationProvider
import software.aws.clientrt.http.feature.HttpDeserialize
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.http.response.HttpResponseContext
import software.aws.clientrt.http.response.TypeInfo
import software.aws.clientrt.http.response.header
import software.aws.clientrt.serde.*
import software.aws.kotlinsdk.AwsServiceException
import software.aws.kotlinsdk.UnknownServiceException
import software.aws.kotlinsdk.http.X_AMZN_REQUEST_ID_HEADER
import software.aws.kotlinsdk.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalStdlibApi::class)
class RestJsonErrorTest {

    class FooError private constructor(builder: BuilderImpl) : AwsServiceException() {
        val headerInt: Int? = builder.headerInt
        val payloadString: String? = builder.payloadString

        companion object {
            fun dslBuilder(): DslBuilder = BuilderImpl()
            operator fun invoke(block: DslBuilder.() -> Unit): FooError = BuilderImpl().apply(block).build()
        }

        interface Builder {
            fun build(): FooError
        }

        interface DslBuilder {
            var headerInt: Int?
            var payloadString: String?
            fun build(): FooError
        }

        private class BuilderImpl() : Builder, DslBuilder {
            override var headerInt: Int? = null
            override var payloadString: String? = null
            override fun build(): FooError = FooError(this)
        }

        override val errorType = ErrorType.Server
    }

    class FooErrorDeserializer : HttpDeserialize {
        companion object {
            val PAYLOAD_STRING_DESCRIPTOR = SdkFieldDescriptor("string", SerialKind.String)
            val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                field(PAYLOAD_STRING_DESCRIPTOR)
            }
        }

        override suspend fun deserialize(response: HttpResponse, provider: DeserializationProvider): FooError {
            val builder = FooError.dslBuilder()
            builder.headerInt = response.headers["X-Test-Header"]?.toInt()

            val payload = response.body.readAll()
            if (payload != null) {
                val deserializer = provider(payload)
                deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                    loop@while (true) {
                        when (findNextFieldIndex()) {
                            PAYLOAD_STRING_DESCRIPTOR.index -> builder.payloadString = deserializeString()
                            null -> break@loop
                            else -> skipValue()
                        }
                    }
                }
            }

            return builder.build()
        }
    }

    @Test
    fun `it throws matching errors`() = runSuspendTest {

        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse { throw NotImplementedError() }
        }
        val client = sdkHttpClient(mockEngine) {
            install(RestJsonError) {
                register("FooError", FooErrorDeserializer(), 502)
            }
        }

        val req = HttpRequestBuilder().build()
        val headers = Headers {
            append("X-Test-Header", "12")
            append(X_AMZN_REQUEST_ID_HEADER, "guid")
            append(X_AMZN_ERROR_TYPE_HEADER_NAME, "FooError")
        }
        val payload = """{"baz":"quux","string":"hello world","message":"server do better next time"}"""
        val body = ByteArrayContent(payload.encodeToByteArray())
        val httpResp = HttpResponse(HttpStatusCode.fromValue(502), headers, body, req)
        val context = HttpResponseContext(httpResp, TypeInfo(Int::class), null)

        val ex = assertFailsWith(FooError::class) {
            client.responsePipeline.execute(context, httpResp.body)
        }

        // verify it pulls out the error details/meta
        assertEquals(ex.errorCode, "FooError")
        assertEquals(ex.requestId, "guid")
        assertEquals(ex.errorMessage, "server do better next time")

        // check it actually deserialized the shape
        assertEquals(ex.headerInt, 12)
        assertEquals(ex.payloadString, "hello world")

        // verify the ProtocolResponse instance was stashed and we can pull out raw protocol details if needed
        assertEquals("12", ex.protocolResponse?.header("X-Test-Header"))
    }

    @Test
    fun `it throws unknown`() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse { throw NotImplementedError() }
        }
        val client = sdkHttpClient(mockEngine) {
            install(RestJsonError) {
                register("FooError", FooErrorDeserializer(), 502)
            }
        }

        val req = HttpRequestBuilder().build()
        val headers = Headers {
            append("X-Test-Header", "12")
            append(X_AMZN_REQUEST_ID_HEADER, "guid")
            append(X_AMZN_ERROR_TYPE_HEADER_NAME, "BarError")
        }
        val payload = """{"baz":"quux","string":"hello world","message":"server do better next time"}"""
        val body = ByteArrayContent(payload.encodeToByteArray())
        val httpResp = HttpResponse(HttpStatusCode.fromValue(502), headers, body, req)
        val context = HttpResponseContext(httpResp, TypeInfo(Int::class), null)

        val ex = assertFailsWith(UnknownServiceException::class) {
            client.responsePipeline.execute(context, httpResp.body)
        }

        // verify it pulls out the error details/meta
        assertEquals(ex.errorCode, "BarError")
        assertEquals(ex.requestId, "guid")
        assertEquals(ex.errorMessage, "server do better next time")
    }
}
