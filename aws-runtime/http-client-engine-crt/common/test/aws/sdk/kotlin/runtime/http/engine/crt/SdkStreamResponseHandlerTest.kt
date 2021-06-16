/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.http.*
import aws.sdk.kotlin.crt.io.byteArrayBuffer
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import kotlinx.coroutines.launch
import kotlin.test.*

class SdkStreamResponseHandlerTest {

    private class MockHttpStream(override val responseStatusCode: Int) : HttpStream {
        var closed: Boolean = false
        override fun activate() {}
        override fun close() { closed = true }
        override fun incrementWindow(size: Int) {}
    }

    private class MockHttpClientConnection : HttpClientConnection {
        var isClosed: Boolean = false
        override fun close() { isClosed = true }
        override fun makeRequest(httpReq: HttpRequest, handler: HttpStreamResponseHandler): HttpStream { throw UnsupportedOperationException("not implemented for test") }
    }

    private val mockConn = MockHttpClientConnection()

    @Test
    fun testWaitSuccessResponse() = runSuspendTest {
        val handler = SdkStreamResponseHandler(mockConn)
        val stream = MockHttpStream(200)
        launch {
            val headers = listOf(
                HttpHeader("foo", "bar"),
                HttpHeader("baz", "qux"),
            )
            handler.onResponseHeaders(stream, 200, HttpHeaderBlock.MAIN.blockType, headers)
            handler.onResponseHeadersDone(stream, HttpHeaderBlock.MAIN.blockType)
        }

        // should be signalled as soon as headers are available
        val resp = handler.waitForResponse()
        assertEquals(HttpStatusCode.OK, resp.status)

        assertTrue(resp.body is HttpBody.Empty)

        assertFalse(mockConn.isClosed)
        handler.onResponseComplete(stream, 0)
        assertTrue(mockConn.isClosed)
    }

    @Test
    fun testWaitNoHeaders() = runSuspendTest {
        val handler = SdkStreamResponseHandler(mockConn)
        val stream = MockHttpStream(200)
        launch {
            handler.onResponseComplete(stream, 0)
        }

        val resp = handler.waitForResponse()
        assertEquals(HttpStatusCode.OK, resp.status)
        assertTrue(mockConn.isClosed)
    }

    @Test
    fun testWaitFailedResponse() = runSuspendTest {
        val handler = SdkStreamResponseHandler(mockConn)
        val stream = MockHttpStream(200)
        launch {
            handler.onResponseComplete(stream, -1)
        }

        // failed engine execution should raise an exception
        assertFails {
            handler.waitForResponse()
        }

        assertTrue(mockConn.isClosed)
    }

    @Test
    fun testRespBodyCreated() = runSuspendTest {
        val handler = SdkStreamResponseHandler(mockConn)
        val stream = MockHttpStream(200)
        launch {
            val headers = listOf(
                HttpHeader("Content-Length", "72")
            )
            handler.onResponseHeaders(stream, 200, HttpHeaderBlock.MAIN.blockType, headers)
            handler.onResponseHeadersDone(stream, HttpHeaderBlock.MAIN.blockType)
        }

        // should be signalled as soon as headers are available
        val resp = handler.waitForResponse()
        assertEquals(HttpStatusCode.OK, resp.status)

        assertEquals(72, resp.body.contentLength)
        assertTrue(resp.body is HttpBody.Streaming)
        val respChan = (resp.body as HttpBody.Streaming).readFrom()
        assertFalse(respChan.isClosedForWrite)

        assertFalse(mockConn.isClosed)
        handler.onResponseComplete(stream, 0)
        assertTrue(mockConn.isClosed)
        assertTrue(respChan.isClosedForWrite)
    }

    @Test
    fun testRespBody() = runSuspendTest {
        val handler = SdkStreamResponseHandler(mockConn)
        val stream = MockHttpStream(200)
        val data = "Fool of a Took! Throw yourself in next time and rid us of your stupidity!"
        launch {
            val headers = listOf(
                HttpHeader("Content-Length", "${data.length}")
            )
            handler.onResponseHeaders(stream, 200, HttpHeaderBlock.MAIN.blockType, headers)
            handler.onResponseHeadersDone(stream, HttpHeaderBlock.MAIN.blockType)
            handler.onResponseBody(stream, byteArrayBuffer(data.encodeToByteArray()))
            handler.onResponseComplete(stream, 0)
        }

        // should be signalled as soon as headers are available
        val resp = handler.waitForResponse()
        assertEquals(HttpStatusCode.OK, resp.status)

        assertEquals(data.length.toLong(), resp.body.contentLength)
        assertTrue(resp.body is HttpBody.Streaming)
        val respChan = (resp.body as HttpBody.Streaming).readFrom()

        assertTrue(mockConn.isClosed)
        assertTrue(respChan.isClosedForWrite)

        assertEquals(data, respChan.readRemaining().decodeToString())
    }
}
