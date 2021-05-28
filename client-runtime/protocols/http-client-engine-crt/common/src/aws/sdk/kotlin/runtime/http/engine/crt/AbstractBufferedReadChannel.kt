/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.io.Buffer
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import software.aws.clientrt.io.SdkBuffer
import software.aws.clientrt.io.bytes
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private data class ClosedSentinel(val cause: Throwable?)

private const val SEGMENT_SIZE = 4096

/**
 * Abstract base class that platform implementations should inherit from
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
internal abstract class AbstractBufferedReadChannel(
    // function invoked every time n bytes are read
    private val onBytesRead: (n: Int) -> Unit
) : BufferedReadChannel {

    // NOTE: the channel is configured as unlimited but will always be constrained by the window size such
    // that there are only ever (WINDOW_SIZE / SEGMENT_SIZE) buffers in-flight at any one time
    // FIXME - this comment is erroneous (for now until we implement emitting full segments), we actually have as many buffers in flight as
    // get written but these will add up to WINDOW_SIZE
    private val segments = Channel<Segment>(Channel.UNLIMITED)

    private val currSegment: AtomicRef<Segment?> = atomic(null)

    private val readOp: AtomicRef<CancellableContinuation<Boolean>?> = atomic(null)

    private val closed: AtomicRef<ClosedSentinel?> = atomic(null)
    private val _availableForRead = atomic(0)

    override val isClosedForWrite: Boolean
        get() = segments.isClosedForSend

    override val isClosedForRead: Boolean
        get() = closed.value != null && segments.isClosedForReceive

    override val availableForRead: Int
        get() = _availableForRead.value

    /**
     * Suspend reading until at least [requested] bytes are available to read or the channel is closed.
     * If the requested amount can be fulfilled immediately this function will return without suspension.
     */
    private suspend fun readSuspend(requested: Int): Boolean {
        // can fulfill immediately without suspension
        if (availableForRead >= requested) return true

        closed.value?.let { closed ->
            // if already closed - rethrow
            closed.cause?.let { rethrowClosed(it) }

            // no more data is coming
            return availableForRead >= requested
        }

        // FIXME - this needs to be a loop?
        return suspendCancellableCoroutine { cont ->
            setReadContinuation(cont)
        }
    }

    private fun setReadContinuation(cont: CancellableContinuation<Boolean>) {
        val current = readOp.value
        check(current == null) { "Read operation already in progress" }
        readOp.update { cont }
    }

    private fun resumeRead() {
        readOp.getAndSet(null)?.let { cont ->
            cont.resume(true)
        }
    }

    /**
     * Decrease the amount of bytes available for reading and notify the callback
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun markBytesConsumed(size: Int) {
        _availableForRead -= size
        onBytesRead(size)
    }

    // FIXME - probably switch to UInt now that unsigned types are stable in 1.5
    override suspend fun readRemaining(limit: Int): ByteArray {
        val buffer = SdkBuffer(minOf(availableForRead, limit))

        val consumed = readAsMuchAsPossible(buffer, limit)

        return if (consumed >= limit) {
            buffer.bytes()
        } else {
            readRemainingSuspend(buffer, limit - consumed)
        }
    }

    private fun readAsMuchAsPossible(dest: SdkBuffer, limit: Int): Int {
        var consumed = 0
        var remaining = limit

        while (availableForRead > 0 && remaining > 0) {
            val segment = currSegment.getAndSet(null) ?: segments.tryReceive().getOrNull() ?: break

            val rc = segment.copyTo(dest, remaining)
            consumed += rc
            remaining = limit - consumed

            markBytesConsumed(rc)

            if (segment.availableForRead > 0) {
                currSegment.update { segment }
            }
        }

        return consumed
    }

    private suspend fun readRemainingSuspend(buffer: SdkBuffer, limit: Int): ByteArray {
        check(currSegment.value == null) { "current segment should be drained already" }

        var consumed = 0

        for (segment in segments) {
            val remaining = limit - consumed
            val rc = segment.copyTo(buffer, remaining)
            consumed += rc

            markBytesConsumed(rc)

            if (remaining <= 0) {
                if (segment.availableForRead > 0) {
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

            if (segment.availableForRead > 0) {
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
            consumed == 0 && closed.value != null -> -1
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
        // FIXME - we could pool these allocations
        val bytesIn = ByteArray(data.len)
        val wc = data.copyTo(bytesIn)
        check(wc == bytesIn.size) { "short read: copied $wc; expected: ${bytesIn.size} " }

        // FIXME - only emit full segments or partial when closed?
        // val buffer = SdkBuffer(minOf(bytes.size, SEGMENT_SIZE))

        val result = segments.trySend(Segment(bytesIn))
        check(result.isSuccess) { "failed to queue segment" }

        // advertise bytes available
        _availableForRead.getAndAdd(bytesIn.size)

        resumeRead()
    }

    override fun cancel(cause: Throwable?): Boolean {
        if (closed.value != null) return false

        closed.update { ClosedSentinel(cause) }

        segments.close()

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
