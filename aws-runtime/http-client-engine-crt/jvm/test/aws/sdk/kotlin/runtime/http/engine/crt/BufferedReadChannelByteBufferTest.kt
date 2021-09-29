/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.smithy.kotlin.runtime.testing.ManualDispatchTestBase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.nio.ByteBuffer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferedReadChannelByteBufferTest : ManualDispatchTestBase() {
    private var totalRead: Int = 0
    private val ch by lazy {
        bufferedReadChannel { size ->
            totalRead += size
        }
    }

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
            val buf = ByteBuffer.allocate(16)

            // should suspend
            val rc = ch.readAvailable(buf)
            expect(5)
            assertEquals(data.length, rc)
            assertEquals(rc, buf.position())
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

            val buf = ByteBuffer.allocate(16)
            // should NOT suspend
            val rc = ch.readAvailable(buf)

            expect(4)
            assertEquals(4, rc)
            assertEquals(rc, buf.position())

            expect(5)
        }

        expect(2)
        yield()
        finish(6)
    }
}
