/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration

/**
 * A value with an expiration
 */
internal data class ExpiringValue<T> (val value: T, val expiresAt: Instant)

/**
 * Expiry aware value
 *
 * @param value The value that expires
 * @param bufferTime The amount of time before the actual expiration time when the value is considered expired. By default
 * the buffer time is zero meaning the value expires at the expiration time. A non-zero buffer time means the value will
 * expire BEFORE the actual expiration.
 * @param clock The clock to use for system time
 */
internal class CachedValue<T> (
    private var value: ExpiringValue<T>? = null,
    private val bufferTime: Duration = Duration.ZERO,
    private val clock: Clock = Clock.System,
) : Closeable {
    constructor(value: T, expiresAt: Instant, bufferTime: Duration = Duration.ZERO, clock: Clock = Clock.System) : this(ExpiringValue(value, expiresAt), bufferTime, clock)

    private val gate = Semaphore(1)
    private val _ref: AtomicRef<ExpiringValue<T>?> = atomic(value)
    private val ref: ExpiringValue<T>?
        get() = _ref.value
    private val closed = atomic(false)

    /**
     * Check if the value is expired or not as compared to the time now
     */
    val isExpired: Boolean
        get() {
            check(!closed.value) { "value is closed" }
            return ref?.let { isExpired(it) } ?: true
        }

    private fun isExpired(value: ExpiringValue<T>): Boolean = clock.now() >= (value.expiresAt - bufferTime)

    /**
     * Get the value if it has not expired yet. Returns null if the value has expired
     */
    fun get(): T? {
        check(!closed.value) { "value is closed" }
        val curr = ref ?: return null

        return if (isExpired(curr)) null else curr.value
    }

    /**
     * Attempt to get the value or refresh it with [initializer] if it is expired
     */
    suspend fun getOrLoad(initializer: suspend () -> ExpiringValue<T>): T = gate.withPermit {
        check(!closed.value) { "value is closed" }

        val curr = ref
        if (curr != null && !isExpired(curr)) {
            return curr.value
        }

        val next = initializer()

        check(!closed.value) { "value is closed" }
        check(_ref.compareAndSet(curr, next)) { "value changed during getOrLoad" }

        return next.value
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) { return }
        _ref.update { null }
    }
}
