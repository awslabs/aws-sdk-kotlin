/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// modeled after https://github.com/ktorio/ktor/blob/78e36790cdbb30313dfbd23b174bffe805d26dca/ktor-io/common/test/io/ktor/utils/io/ByteChannelTestBase.kt
// but implemented using [kotlinx-coroutines-test](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test)
// rather than rolling our own dummy coroutines dispatcher
/**
 * Test suspend functions with precise control over coordination. This allows testing that suspension happens at the
 * expected point in time using the provided [expect] function, e.g.:
 *
 * ```
 * class TestFoo : ManualDispatchTestBase() {
 *     @Test
 *     fun testFoo() = runTest {
 *         expect(1)
 *         launch {
 *             expect(3)
 *             someFunctionThatShouldSuspend()
 *             expect(5)
 *         }
 *
 *         expect(2)
 *         yield()
 *         expect(4)
 *         unblockSuspendedFunction()
 *         yield()
 *         finish(6)
 *     }
 * }
 * ```
 *
 * Explicitly yielding or hitting a natural suspension point will run the next continuation queued
 */
@OptIn(ExperimentalCoroutinesApi::class)
public abstract class ManualDispatchTestBase {
    private var current = 0

    /**
     * Execute a test with the provided (test) coroutine scope. Any calls to `launch` or `async`
     * will not be executed immediately and instead be scheduled for dispatch. Explicit calls to `yield()`
     * will advance the dispatcher.
     */
    protected fun runTest(block: suspend TestCoroutineScope.() -> Unit) {
        runBlockingTest {
            // ensure launch/async calls are coordinated with yield() points
            pauseDispatcher()
            block()
        }
    }

    /**
     * Assert the current execution point and increment the count
     */
    protected fun expect(n: Int) {
        val next = current + 1
        assertNotEquals(0, next, "Already finished")
        assertEquals(n, next, "Invalid test state")
        current = next
    }

    /**
     * Assert the current execution point and mark the test finished. Any further
     * calls to [expect] will fail.
     */
    protected fun finish(n: Int) {
        expect(n)
        current = -1
    }
}
