/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.io.byteArrayBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.lang.RuntimeException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

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

    private fun BufferedReadChannel.write(str: String) {
        write(byteArrayBuffer(str.encodeToByteArray()))
    }

    class TestException : RuntimeException("test exception")

    @AfterTest
    fun finish() {
        ch.cancel(CancellationException("Test finished"))
    }

    @Test
    fun testReadBeforeAvailable() = runTest {
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
    fun testReadToEmpty() = runTest {
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
        val rc2 = ch.readAvailable(buf, offset = 2)
        assertEquals(6, rc2)
        finish(7)
    }
}
