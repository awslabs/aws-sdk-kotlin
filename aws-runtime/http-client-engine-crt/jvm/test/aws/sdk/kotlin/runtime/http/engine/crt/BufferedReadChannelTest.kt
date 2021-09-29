/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.io.byteArrayBuffer
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.io.readByte
import aws.smithy.kotlin.runtime.testing.ManualDispatchTestBase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.lang.RuntimeException
import kotlin.test.*

internal fun BufferedReadChannel.write(bytes: ByteArray) {
    write(byteArrayBuffer(bytes))
}

internal fun BufferedReadChannel.write(str: String) {
    write(str.encodeToByteArray())
}

// FIXME - move all these tests to common when coroutines-test is available in KMP
// see https://github.com/Kotlin/kotlinx.coroutines/issues/1996

// test suite adapted from: https://github.com/ktorio/ktor/blob/main/ktor-io/common/test/io/ktor/utils/io/ByteBufferChannelScenarioTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class BufferedReadChannelTest : ManualDispatchTestBase() {
    private var totalRead: Int = 0
    private val ch by lazy {
        bufferedReadChannel { size ->
            totalRead += size
        }
    }

    private fun BufferedReadChannel.write(bytes: ByteArray) {
        write(byteArrayBuffer(bytes))
    }

    private fun BufferedReadChannel.write(str: String) {
        write(str.encodeToByteArray())
    }

    class TestException : RuntimeException("test exception")

    @AfterTest
    fun finish() {
        ch.cancel(CancellationException("Test finished"))
    }

    @Test
    fun testReadBeforeAvailable() = runTest {
        // test readAvailable() suspends when no data is available
        expect(1)

        val data = "1234"

        launch {
            expect(3)
            val buf = ByteArray(16)

            // should suspend
            val rc = ch.readAvailable(buf)
            expect(5)
            assertEquals(data.length, rc)
        }

        expect(2)
        yield()

        expect(4)

        // read continuation should be queued to resume
        ch.write(data)
        yield()

        finish(6)
    }

    @Test
    fun testReadBeforeAvailableFully() = runTest {
        // test readFully() suspends when no data is available
        expect(1)

        val data = "1234"

        launch {
            expect(3)
            val buf = ByteArray(16)

            // should suspend
            ch.readFully(buf, length = 4)
            expect(5)
        }

        expect(2)
        yield()

        expect(4)

        // read continuation should be queued to resume
        ch.write(data)
        yield()

        finish(6)
    }

    @Test
    fun testReadAfterAvailable() = runTest {
        // test readAvailable() does NOT suspend when data is available
        expect(1)
        ch.write("1234")
        launch {
            expect(3)

            val buf = ByteArray(16)
            // should NOT suspend
            val rc = ch.readAvailable(buf)

            expect(4)
            assertEquals(4, rc)

            expect(5)
        }

        expect(2)
        yield()
        finish(6)
    }

    @Test
    fun testReadAfterAvailableFully() = runTest {
        // test readFully() does NOT suspend when data is available to satisfy the request
        expect(1)

        ch.write("1234")

        launch {
            expect(3)

            val buf = ByteArray(16)
            // should NOT suspend
            ch.readFully(buf, length = 4)

            expect(4)
        }

        expect(2)
        yield()

        finish(5)
    }

    @Test
    fun testReadFullySuspends() = runTest {
        // test readFully() suspends when not enough data is available to satisfy the request
        expect(1)

        ch.write("1234")

        launch {
            expect(3)

            val buf = ByteArray(16)
            // should suspend
            ch.readFully(buf, length = 8)

            expect(6)
        }

        expect(2)
        yield()
        expect(4)
        ch.write("5678")

        expect(5)
        yield()

        finish(7)
    }

    @Test
    fun testReadToEmpty() = runTest {
        // test readAvailable() does not suspend when length is zero
        // (in practice you wouldn't set 0 but it could happen when combined with an offset)
        expect(1)

        val buf = ByteArray(16)
        val rc = ch.readAvailable(buf, length = 0)
        expect(2)
        assertEquals(0, rc)

        finish(3)
    }

    @Test
    fun testReadToEmptyFromFailedChannel() = runTest {
        expect(1)
        ch.cancel(TestException())
        val buf = ByteArray(16)
        val rc = ch.readAvailable(buf, length = 0)
        expect(2)
        assertEquals(-1, rc)
        finish(3)
    }

    @Test
    fun testReadToEmptyFromClosedChannel() = runTest {
        expect(1)
        ch.close()
        val buf = ByteArray(16)
        val rc = ch.readAvailable(buf, length = 0)
        expect(2)
        assertEquals(-1, rc)
        finish(3)
    }

    @Test
    fun testReadFullyFromFailedChannel() = runTest {
        expect(1)
        ch.cancel(TestException())
        assertFails {
            val buf = ByteArray(1)
            ch.readFully(buf)
        }
        finish(2)
    }

    @Test
    fun testReadFullyFromClosedChannel() = runTest {
        expect(1)
        ch.close()
        assertFails {
            val buf = ByteArray(1)
            ch.readFully(buf)
        }
        finish(2)
    }

    @Test
    fun readPartialSegment() = runTest {
        expect(1)
        ch.write("1234")
        launch {
            expect(4)
            ch.write("5678")
            expect(5)
        }
        expect(2)
        val buf = ByteArray(16)
        val rc1 = ch.readAvailable(buf, length = 2)
        expect(3)
        assertEquals(2, rc1)
        yield()
        expect(6)
        // second read should consume the remainder of the first write "34" + the entire second write
        val rc2 = ch.readAvailable(buf, offset = 2)
        assertEquals(6, rc2)
        finish(7)
    }

    @Test
    fun testReadState() = runTest {
        assertFalse(ch.isClosedForWrite)
        assertFalse(ch.isClosedForRead)
        assertEquals(0, ch.availableForRead)
        ch.write("1234")
        assertEquals(4, ch.availableForRead)
        ch.close()
        assertTrue(ch.isClosedForWrite)
        assertFalse(ch.isClosedForRead)

        val buf = ByteArray(16)
        val rc = ch.readAvailable(buf)
        assertEquals(4, rc)

        assertEquals(0, ch.availableForRead)
        assertTrue(ch.isClosedForRead)
    }

    @Test
    fun testReadRemaining() = runTest {
        expect(1)
        ch.write("1234")
        launch {
            expect(3)
            val buf = ch.readRemaining()
            assertEquals("1234", buf.decodeToString())
            expect(5)
        }

        expect(2)
        yield()
        expect(4)
        ch.close()
        yield()
        finish(6)
    }

    @Test
    fun testReadRemainingLimit() = runTest {
        // should test partial segment reading
        expect(1)
        ch.write("123")
        ch.write("456")
        ch.write("789")
        launch {
            expect(3)
            // should NOT suspend because of limit
            val buf = ch.readRemaining(limit = 5)
            assertEquals("12345", buf.decodeToString())
            expect(4)
        }

        expect(2)
        yield()

        expect(5)
        ch.close()

        expect(6)

        assertEquals(4, ch.availableForRead)
        // should NOT suspend because the channel is closed
        val buf = ch.readRemaining()
        assertEquals("6789", buf.decodeToString())

        finish(7)
    }

    @Test
    fun testReadInProgress() = runTest {
        expect(1)
        launch {
            expect(3)
            val buf = ByteArray(16)
            ch.readAvailable(buf)
        }
        expect(2)
        yield()
        expect(4)

        assertFailsWith<IllegalStateException>("Read operation already in progress") {
            val buf = ByteArray(16)
            ch.readAvailable(buf)
        }
        ch.close()
        finish(5)
    }

    @Test
    fun testReadFullyEof() = runTest {
        expect(1)
        ch.write("1234")
        val buf = ByteArray(16)
        launch {
            expect(3)
            assertFailsWith<ClosedReceiveChannelException>("Unexpeced EOF: expected 12 more bytes") {
                ch.readFully(buf)
            }
        }
        expect(2)
        yield()
        expect(4)

        ch.close()
        finish(5)
    }

    @Test
    fun testResumeReadFromFailedChannel() = runTest {
        expect(1)

        launch {
            expect(3)
            ch.cancel(TestException())
        }

        expect(2)
        val buf = ByteArray(16)
        assertFailsWith<TestException> {
            // should suspend and fail with the exception when resumed
            ch.readAvailable(buf)
        }
        finish(4)
    }

    @Test
    fun testResumeReadAvailableFromClosedChannelNoContent() = runTest {
        expect(1)

        launch {
            expect(3)
            ch.close()
        }

        expect(2)
        val buf = ByteArray(16)
        val rc = ch.readAvailable(buf)
        assertEquals(-1, rc)
        finish(4)
    }

    @Test
    fun testReadAndWriteFully() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        ch.write(bytes)
        val buf = ByteArray(5)
        ch.readFully(buf)
        assertTrue { buf.contentEquals(bytes) }

        ch.write(bytes)
        val buf2 = ByteArray(4)
        ch.readFully(buf2)
        assertEquals(1, ch.availableForRead)
        assertEquals(5, ch.readByte())
        ch.close()

        assertFails {
            ch.readFully(buf)
        }
    }

    @Test
    fun testLargeTransfer() = runTest {
        val size = 262144 + 512
        launch {
            ch.write(ByteArray(size))
            ch.close()
        }

        val buf = ch.readRemaining()
        assertEquals(size, buf.size)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testWriteRaceCondition() = runSuspendTest {
        var totalBytes = 0
        val channel = bufferedReadChannel { size -> totalBytes += size }
        val writeJob = GlobalScope.async {
            try {
                val data = byteArrayOf(2)
                repeat(1_000_000) {
                    channel.write(data)
                }
                channel.close()
            } catch (ex: Exception) {
                channel.cancel(ex)
                throw ex
            }
        }

        val readJob = GlobalScope.async {
            channel.readRemaining()
        }

        writeJob.await()
        readJob.await()
        assertEquals(1_000_000, totalBytes)
    }
}
