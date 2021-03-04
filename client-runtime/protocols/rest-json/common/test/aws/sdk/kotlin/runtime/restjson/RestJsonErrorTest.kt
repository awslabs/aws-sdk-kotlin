/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.restjson

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.UnknownServiceErrorException
import aws.sdk.kotlin.runtime.http.X_AMZN_REQUEST_ID_HEADER
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.http.*
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.operation.HttpDeserialize
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.http.operation.UnitDeserializer
import software.aws.clientrt.http.operation.UnitSerializer
import software.aws.clientrt.http.operation.context
import software.aws.clientrt.http.operation.roundTrip
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.http.response.header
import software.aws.clientrt.serde.*
import software.aws.clientrt.serde.json.JsonSerialName
import software.aws.clientrt.serde.json.JsonSerdeProvider
import kotlin.test.*

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

    class FooErrorDeserializer(val provider: DeserializationProvider) : HttpDeserialize<FooError> {
        companion object {
            val PAYLOAD_STRING_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, JsonSerialName("string"))
            val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
                field(PAYLOAD_STRING_DESCRIPTOR)
            }
        }

        override suspend fun deserialize(context: ExecutionContext, response: HttpResponse): FooError {
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

        val req = HttpRequestBuilder().build()
        val headers = Headers {
            append("X-Test-Header", "12")
            append(X_AMZN_REQUEST_ID_HEADER, "guid")
            append(X_AMZN_ERROR_TYPE_HEADER_NAME, "FooError")
        }
        val payload = """{"baz":"quux","string":"hello world","message":"server do better next time"}"""
        val body = ByteArrayContent(payload.encodeToByteArray())
        val httpResp = HttpResponse(HttpStatusCode.fromValue(502), headers, body, req)

        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse { return httpResp }
        }

        val client = sdkHttpClient(mockEngine)

        val op = SdkHttpOperation.build<Unit, Unit> {
            serializer = UnitSerializer
            deserializer = UnitDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"
            }
        }

        val provider = JsonSerdeProvider()
        op.install(RestJsonError) {
            register("FooError", FooErrorDeserializer(provider::deserializer), 502)
        }
        val ex = assertFailsWith(FooError::class) {
            op.roundTrip(client, Unit)
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

        val req = HttpRequestBuilder().build()
        val headers = Headers {
            append("X-Test-Header", "12")
            append(X_AMZN_REQUEST_ID_HEADER, "guid")
            append(X_AMZN_ERROR_TYPE_HEADER_NAME, "BarError")
        }
        val payload = """{"baz":"quux","string":"hello world","message":"server do better next time"}"""
        val body = ByteArrayContent(payload.encodeToByteArray())
        val httpResp = HttpResponse(HttpStatusCode.fromValue(502), headers, body, req)

        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse { return httpResp }
        }

        val client = sdkHttpClient(mockEngine)

        val op = SdkHttpOperation.build<Unit, Unit> {
            serializer = UnitSerializer
            deserializer = UnitDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"
            }
        }

        val provider = JsonSerdeProvider()
        op.install(RestJsonError) {
            register("FooError", FooErrorDeserializer(provider::deserializer), 502)
        }

        val ex = assertFailsWith(UnknownServiceErrorException::class) {
            op.roundTrip(client, Unit)
        }

        // verify it pulls out the error details/meta
        assertEquals(ex.errorCode, "BarError")
        assertEquals(ex.requestId, "guid")
        assertEquals(ex.errorMessage, "server do better next time")
    }

    @Test
    fun `it handles http status code matching`() {
        // positive cases
        assertTrue(HttpStatusCode.OK.matches(null)) // Expected is null, actual is success
        assertTrue(HttpStatusCode.OK.matches(HttpStatusCode.OK))
        assertTrue(HttpStatusCode.OK.matches(HttpStatusCode.Created))

        // negative cases
        assertFalse(HttpStatusCode.OK.matches(HttpStatusCode.BadGateway))
        assertFalse(HttpStatusCode.BadRequest.matches(null)) // Expected is null, actual is error
    }

    @Test
    fun `it handles non-json payloads`() = runSuspendTest {
        // the service itself may talk rest-json but errors (like signature mismatch) may return unknown payloads
        val req = HttpRequestBuilder().build()
        val headers = Headers {
            append("X-Test-Header", "12")
            append(X_AMZN_REQUEST_ID_HEADER, "guid")
            append(X_AMZN_ERROR_TYPE_HEADER_NAME, "BarError")
        }
        val payload = """
           <AccessDeniedException>
                <Message>Could not determine service to authorize</Message>
           </AccessDeniedException>
        """.trimIndent()

        val body = ByteArrayContent(payload.encodeToByteArray())
        val httpResp = HttpResponse(HttpStatusCode.fromValue(502), headers, body, req)

        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse { return httpResp }
        }

        val client = sdkHttpClient(mockEngine)

        val op = SdkHttpOperation.build<Unit, Unit> {
            serializer = UnitSerializer
            deserializer = UnitDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"
            }
        }

        val provider = JsonSerdeProvider()
        op.install(RestJsonError) {
            register("FooError", FooErrorDeserializer(provider::deserializer), 502)
        }

        val ex = assertFailsWith(UnknownServiceErrorException::class) {
            op.roundTrip(client, Unit)
        }

        // verify it pulls out the error details/meta
        assertEquals(ex.errorCode, "")
        assertEquals(ex.requestId, "guid")
        assertEquals(ex.message, "failed to parse response as restJson protocol error")
    }
}
