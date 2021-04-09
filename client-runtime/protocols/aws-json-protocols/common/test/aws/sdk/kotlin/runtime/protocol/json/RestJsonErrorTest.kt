/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.json

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.UnknownServiceErrorException
import aws.sdk.kotlin.runtime.http.X_AMZN_REQUEST_ID_HEADER
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import software.aws.clientrt.ServiceErrorMetadata
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
import software.aws.clientrt.http.request.HttpRequest
import software.aws.clientrt.http.response.HttpCall
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.http.response.header
import software.aws.clientrt.serde.*
import software.aws.clientrt.serde.json.JsonSerdeProvider
import software.aws.clientrt.time.Instant
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
class RestJsonErrorTest {

    class FooError private constructor(builder: BuilderImpl) : AwsServiceException() {
        val headerInt: Int? = builder.headerInt
        val payloadString: String? = builder.payloadString
        init {
            sdkErrorMetadata.attributes[ServiceErrorMetadata.ErrorType] = ErrorType.Server
        }

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
    }

    class FooErrorDeserializer(val provider: DeserializationProvider) : HttpDeserialize<FooError> {
        companion object {
            val PAYLOAD_STRING_DESCRIPTOR = SdkFieldDescriptor("string", SerialKind.String)
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

        val headers = Headers {
            append("X-Test-Header", "12")
            append(X_AMZN_REQUEST_ID_HEADER, "guid")
            append(X_AMZN_ERROR_TYPE_HEADER_NAME, "FooError")
        }
        val payload = """{"baz":"quux","string":"hello world","message":"server do better next time"}"""
        val body = ByteArrayContent(payload.encodeToByteArray())
        val httpResp = HttpResponse(HttpStatusCode.fromValue(502), headers, body)

        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val now = Instant.now()
                return HttpCall(request, httpResp, now, now)
            }
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
        assertEquals("FooError", ex.sdkErrorMetadata.errorCode)
        assertEquals("guid", ex.sdkErrorMetadata.requestId)
        // the exception has no modeled "message" field, ensure we can still get at it through the metadata
        assertEquals("server do better next time", ex.sdkErrorMetadata.errorMessage)

        // check it actually deserialized the shape
        assertEquals(12, ex.headerInt)
        assertEquals("hello world", ex.payloadString)

        // verify the ProtocolResponse instance was stashed and we can pull out raw protocol details if needed
        assertEquals("12", ex.sdkErrorMetadata.protocolResponse.header("X-Test-Header"))
    }

    @Test
    fun `it throws unknown`() = runSuspendTest {
        val headers = Headers {
            append("X-Test-Header", "12")
            append(X_AMZN_REQUEST_ID_HEADER, "guid")
            append(X_AMZN_ERROR_TYPE_HEADER_NAME, "BarError")
        }
        val payload = """{"baz":"quux","string":"hello world","message":"server do better next time"}"""
        val body = ByteArrayContent(payload.encodeToByteArray())
        val httpResp = HttpResponse(HttpStatusCode.fromValue(502), headers, body)

        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val now = Instant.now()
                return HttpCall(request, httpResp, now, now)
            }
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
        assertEquals("BarError", ex.sdkErrorMetadata.errorCode)
        assertEquals("guid", ex.sdkErrorMetadata.requestId)
        assertEquals("server do better next time", ex.message)
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
        val httpResp = HttpResponse(HttpStatusCode.fromValue(502), headers, body)

        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val now = Instant.now()
                return HttpCall(request, httpResp, now, now)
            }
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
        assertNull(ex.sdkErrorMetadata.errorCode)
        assertEquals("guid", ex.sdkErrorMetadata.requestId)
        assertEquals("failed to parse response as Json protocol error", ex.message)
    }
}
