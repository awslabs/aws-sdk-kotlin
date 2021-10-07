/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

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
@OptIn(ExperimentalTime::class)
internal class CachedValue<T> (
    private var value: ExpiringValue<T>? = null,
    private val bufferTime: Duration = Duration.seconds(0),
    private val clock: Clock = Clock.System
) {
    constructor(value: T, expiresAt: Instant, bufferTime: Duration = Duration.seconds(0), clock: Clock = Clock.System) : this(ExpiringValue(value, expiresAt), bufferTime, clock)
    private val mu = Mutex()

    /**
     * Check if the value is expired or not as compared to the time [now]
     */
    suspend fun isExpired(): Boolean = mu.withLock { isExpiredUnlocked() }

    private fun isExpiredUnlocked(): Boolean {
        val curr = value ?: return true
        return clock.now() >= (curr.expiresAt - bufferTime)
    }

    /**
     * Get the value if it has not expired yet. Returns null if the value has expired
     */
    suspend fun get(): T? = mu.withLock {
        if (!isExpiredUnlocked()) return value!!.value else null
    }

    /**
     * Attempt to get the value or refresh it with [initializer] if it is expired
     */
    suspend fun getOrLoad(initializer: suspend () -> ExpiringValue<T>): T = mu.withLock {
        if (!isExpiredUnlocked()) return@withLock value!!.value

        val refreshed = initializer().also { value = it }
        return refreshed.value
    }
}
