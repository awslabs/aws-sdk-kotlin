/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.io.Buffer
import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.bytes
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class ClosedSentinel(val cause: Throwable?)

/**
 * Abstract base class that platform implementations should inherit from
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
internal abstract class AbstractBufferedReadChannel(
    // function invoked every time n bytes are read
    private val onBytesRead: (n: Int) -> Unit
) : BufferedReadChannel {

    // NOTE: the channel is configured as unlimited but will always be constrained by the window size such
    // that there are only ever WINDOW_SIZE _bytes_ in-flight at any given time
    private val segments = Channel<Segment>(Channel.UNLIMITED)

    private val currSegment: AtomicRef<Segment?> = atomic(null)

    private val readOp: AtomicRef<CancellableContinuation<Boolean>?> = atomic(null)

    private val _closed: AtomicRef<ClosedSentinel?> = atomic(null)
    protected val closed: ClosedSentinel?
        get() = _closed.value

    private val _availableForRead = atomic(0)

    override val isClosedForWrite: Boolean
        get() = segments.isClosedForSend

    override val isClosedForRead: Boolean
        get() = closed != null && segments.isClosedForReceive

    override val availableForRead: Int
        get() = _availableForRead.value

    /**
     * Suspend reading until at least [requested] bytes are available to read or the channel is closed.
     * If the requested amount can be fulfilled immediately this function will return without suspension.
     */
    protected suspend fun readSuspend(requested: Int): Boolean {
        // can fulfill immediately without suspension
        if (availableForRead >= requested) return true

        closed?.let { closed ->
            // if already closed - rethrow
            closed.cause?.let { rethrowClosed(it) }

            // no more data is coming
            return availableForRead >= requested
        }

        return suspendCancellableCoroutine { cont ->
            setReadContinuation(cont)
        }
    }

    private fun setReadContinuation(cont: CancellableContinuation<Boolean>) {
        val success = readOp.compareAndSet(null, cont)
        check(success) { "Read operation already in progress" }
    }

    private fun resumeRead() {
        readOp.getAndSet(null)?.resume(true)
    }

    /**
     * Decrease the amount of bytes available for reading and notify the callback
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun markBytesConsumed(size: Int) {
        // NOTE: +/- operators ARE atomic
        _availableForRead -= size
        onBytesRead(size)
    }

    override suspend fun readRemaining(limit: Int): ByteArray {
        val buffer = SdkByteBuffer(minOf(availableForRead, limit).toULong())

        val consumed = readAsMuchAsPossible(buffer, limit)

        return if (consumed >= limit) {
            buffer.bytes()
        } else {
            readRemainingSuspend(buffer, limit - consumed)
        }
    }

    protected fun readAsMuchAsPossible(dest: SdkByteBuffer, limit: Int): Int {
        var consumed = 0
        var remaining = limit

        while (availableForRead > 0 && remaining > 0) {
            val segment = currSegment.getAndSet(null) ?: segments.tryReceive().getOrNull() ?: break

            val rc = segment.copyTo(dest, remaining)
            consumed += rc
            remaining = limit - consumed

            markBytesConsumed(rc)

            if (segment.readRemaining > 0u) {
                currSegment.update { segment }
            }
        }

        return consumed
    }

    private suspend fun readRemainingSuspend(buffer: SdkByteBuffer, limit: Int): ByteArray {
        check(currSegment.value == null) { "current segment should be drained already" }

        var consumed = 0

        for (segment in segments) {
            val remaining = limit - consumed
            val rc = segment.copyTo(buffer, remaining)
            consumed += rc

            markBytesConsumed(rc)

            if (remaining <= 0) {
                if (segment.readRemaining > 0u) {
                    currSegment.update { segment }
                }
                break
            }
        }

        return buffer.bytes()
    }

    private fun readAsMuchAsPossible(dest: ByteArray, offset: Int, length: Int): Int {
        var consumed = 0
        var currOffset = offset
        var remaining = length

        while (availableForRead > 0 && remaining > 0) {
            val segment = currSegment.getAndSet(null) ?: segments.tryReceive().getOrNull() ?: break

            val rc = segment.copyTo(dest, currOffset, remaining)
            consumed += rc
            currOffset += rc
            remaining = length - consumed

            markBytesConsumed(rc)

            if (segment.readRemaining > 0u) {
                currSegment.update { segment }
            }
        }

        return consumed
    }

    override suspend fun readFully(sink: ByteArray, offset: Int, length: Int) {
        val rc = readAsMuchAsPossible(sink, offset, length)
        if (rc < length) {
            readFullySuspend(sink, offset + rc, length - rc)
        }
    }

    private suspend fun readFullySuspend(dest: ByteArray, offset: Int, length: Int) {
        var consumed = 0
        var currOffset = offset
        var remaining = length

        do {
            if (!readSuspend(1)) {
                throw ClosedReceiveChannelException("Unexpeced EOF: expected $remaining more bytes")
            }

            consumed += readAsMuchAsPossible(dest, currOffset, remaining)
            currOffset += consumed
            remaining -= consumed
        } while (remaining > 0)
    }

    override suspend fun readAvailable(sink: ByteArray, offset: Int, length: Int): Int {
        val consumed = readAsMuchAsPossible(sink, offset, length)
        return when {
            consumed == 0 && closed != null -> -1
            consumed > 0 || length == 0 -> consumed
            else -> readAvailableSuspend(sink, offset, length)
        }
    }

    private suspend fun readAvailableSuspend(dest: ByteArray, offset: Int, length: Int): Int {
        if (!readSuspend(1)) {
            return -1
        }
        return readAvailable(dest, offset, length)
    }

    override fun write(data: Buffer) {
        // TODO - we could pool these allocations
        val bytesIn = ByteArray(data.len)
        val wc = data.copyTo(bytesIn)
        check(wc == bytesIn.size) { "short read: copied $wc; expected: ${bytesIn.size} " }

        // TODO - only emit full segments or partial when closed?

        val segment = newReadableSegment(bytesIn)
        val result = segments.trySend(segment)
        check(result.isSuccess) { "failed to queue segment" }

        // advertise bytes available
        _availableForRead.getAndAdd(bytesIn.size)

        resumeRead()
    }

    override suspend fun awaitContent() {
        readSuspend(1)
    }

    override fun cancel(cause: Throwable?): Boolean {
        val success = _closed.compareAndSet(null, ClosedSentinel(cause))
        if (!success) return false

        segments.close(cause)

        readOp.getAndSet(null)?.let { cont ->
            if (cause != null) {
                cont.resumeWithException(cause)
            } else {
                cont.resume(availableForRead > 0)
            }
        }

        return true
    }

    override fun close() {
        cancel(null)
    }

    private fun rethrowClosed(cause: Throwable): Nothing {
        throw cause
    }
}
