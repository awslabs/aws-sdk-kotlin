/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import aws.sdk.kotlin.crt.io.Buffer
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.suspendCancellableCoroutine
import software.aws.clientrt.io.SdkBuffer
import software.aws.clientrt.io.SdkByteReadChannel
import software.aws.clientrt.io.bytes
import software.aws.clientrt.io.writeFully
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private data class ClosedSentinel(val cause: Throwable?)

private const val SEGMENT_SIZE = 4096

internal data class Segment(
    private val buffer: ByteArray,
    private var readHead: Int = 0,
) {
    val availableForRead: Int
        get() = buffer.size - readHead

    /**
     * Attempt to copy up to [length] bytes into [dest] starting at [offset]
     * Returns the number of bytes actually copied which may be less than requested
     */
    fun copyTo(dest: ByteArray, offset: Int = 0, length: Int = dest.size - offset): Int {
        check(availableForRead > 0) { "nothing left to read from segment" }

        val rc = minOf(length, availableForRead)

        val endIdxExclusive = readHead + rc + 1
        buffer.copyInto(dest, offset, readHead, endIdxExclusive)
        readHead += rc
        return rc
    }

    fun copyTo(dest: SdkBuffer, limit: Int = Int.MAX_VALUE): Int {
        check(availableForRead > 0) { "nothing left to read from segment" }
        val wc = minOf(buffer.size - readHead, limit)
        dest.writeFully(buffer, offset = readHead, length = wc)
        readHead += wc
        return wc
    }
}


internal expect fun bufferedReadChannel(onBytesRead: (n: Int) -> Unit): AbstractBufferedReadChannel

/**
 * A buffered [SdkByteReadChannel] that can always satisfy writing without blocking / suspension
 */
@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
internal abstract class AbstractBufferedReadChannel(
    // function invoked every time n bytes are read
    private val onBytesRead: (n: Int) -> Unit
): SdkByteReadChannel {

    // NOTE: the channel is configured as unlimited but will always be constrained by the window size such
    // that there are only ever (WINDOW_SIZE / SEGMENT_SIZE) buffers in-flight at any one time
    // FIXME - this comment is erroneous (for now)
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
     * suspend reading until at least [requested] bytes are available to read or the channel is closed
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
        readOp.getAndSet(null)?.resume(true)
    }

    override suspend fun readRemaining(limit: Int): ByteArray {
        val buffer = SdkBuffer(maxOf(availableForRead, SEGMENT_SIZE))

        var consumed = 0

        // drain any partial reads
        currSegment.getAndSet(null)?.let { segment ->
            consumed += segment.copyTo(buffer, limit)
            markBytesConsumed(consumed)

            if (segment.availableForRead > 0) {
                // this should only ever happen if limit < segment.size, in which case we wouldn't pull
                // anything off the segment channel anyway
                currSegment.update { segment }
            }
        }


        return if (consumed >= limit) {
            buffer.bytes()
        }else {
            readRemainingSuspend(buffer, limit - consumed)
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

        do {
            val segment = currSegment.getAndSet(null) ?: segments.tryReceive().getOrNull() ?: break

            consumed += segment.copyTo(dest, currOffset)
            currOffset += consumed

            val remaining = length - consumed

            if (segment.availableForRead > 0) {
                currSegment.update { segment }
            }

        }while (availableForRead > 0 && remaining > 0)

        markBytesConsumed(consumed)
        return consumed
    }

    override suspend fun readFully(sink: ByteArray, offset: Int, length: Int) {
        val rc = readAsMuchAsPossible(sink, offset, length)
        if (rc < length) {
            readFullySuspend(sink, offset + rc, length - rc)
        }
    }

    private suspend fun readFullySuspend(dest: ByteArray, offset: Int, length: Int){
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

        } while(remaining > 0)
    }

    override suspend fun readAvailable(sink: ByteArray, offset: Int, length: Int): Int {
        readSuspend(1)
        return readAsMuchAsPossible(sink, offset, length)
    }

    fun write(data: Buffer) {
        val bytesIn = data.readAll()
        // FIXME - only emit full segments or partial when closed
        // val buffer = SdkBuffer(minOf(bytes.size, SEGMENT_SIZE))

        val result = segments.trySend(Segment(bytesIn))
        check(result.isSuccess) { "failed to queue segment" }

        // advertise bytes available
        _availableForRead.getAndAdd(bytesIn.size)

        resumeRead()
    }

    override fun cancel(cause: Throwable?): Boolean {
        if (closed.value != null) return false

        closed.update { ClosedSentinel(null) }

        readOp.getAndSet(null)?.let { cont ->
            if (cause != null) {
                cont.resumeWithException(cause)
            }else {
                cont.resume(availableForRead > 0)
            }
        }
        segments.close()

        return true
    }

    override fun close() {
        cancel(null)
    }

    private fun rethrowClosed(cause: Throwable): Nothing {
        throw cause
    }

}
