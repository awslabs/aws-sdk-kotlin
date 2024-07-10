/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.glacier.internal

import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.hashing.HashFunction
import aws.smithy.kotlin.runtime.hashing.Sha256
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.toHttpBody
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.SdkByteChannel
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.text.encoding.encodeToHex
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.fail

private const val MEGABYTE = 1024 * 1024

class TreeHasherTest {
    @Test
    fun testCalculateHashes() = runTest {
        val chunk1 = byteArrayOf(1, 2, 3)
        val chunk2 = byteArrayOf(4, 5, 6)
        val payload = chunk1 + chunk2

        val fullHash = byteArrayOf(7, 9, 11) // Each element added once (thus, ∑(c[n] + 1))
        val treeHash = byteArrayOf(9, 11, 13) // Elements added twice (thus, ∑(c[n] + 2))
        val chunkSize = 3
        val hasher = TreeHasherImpl(chunkSize) { RollingSumHashFunction(chunkSize) }

        val body = HttpBody.fromBytes(payload)
        val hashes = hasher.calculateHashes(body)

        assertContentEquals(fullHash, hashes.fullHash)
        assertContentEquals(treeHash, hashes.treeHash)
    }

    private val payloadBytes = "abcdefghijklmnopqrstuvwxyz".encodeToByteArray() // 26 bytes
    private suspend fun testCalculateHashesWithStream(stream: ByteStream) {
        val hasher = TreeHasherImpl(MEGABYTE, ::Sha256)
        val hashes = hasher.calculateHashes(stream.toHttpBody())

        assertEquals("74df7872289a84fa31b6ae4cfdbac34ef911cfe9357e842c600a060da6a899ae", hashes.fullHash.encodeToHex())
        assertEquals("a1c6d421d75f727ce97e6998ab79e6a1cc08ee9502f541f9c5748b462c4dc83f", hashes.treeHash.encodeToHex())
    }

    @Test
    fun testCalculateHashesWithChannelStream() = runTest {
        val byteStream = object : ByteStream.ChannelStream() {
            override fun readFrom(): SdkByteReadChannel {
                val byteChannel = SdkByteChannel()
                async {
                    withTimeout(10_000) {
                        // For sanity, bail out after 10s
                        (0 until MEGABYTE).forEach {
                            // This will yield len(payloadBytes) megabytes of content
                            val source = SdkBuffer().apply { write(payloadBytes) }
                            byteChannel.write(source)
                        }
                    }
                    byteChannel.close()
                }
                return byteChannel
            }
        }

        testCalculateHashesWithStream(byteStream)
    }

    @Test
    fun testCalculateHashesWithSourceStream() = runTest {
        val source = object : SdkSource {
            private var count = 0
            private var buffer = SdkBuffer()
            override fun close() {}
            override fun read(sink: SdkBuffer, limit: Long): Long {
                if (count == MEGABYTE) return -1L

                return if (buffer.size > 0L) {
                    // had left over from previous read
                    val rc = minOf(limit, buffer.size)
                    sink.write(buffer, rc)
                    rc
                } else {
                    // refill
                    buffer.write(payloadBytes)
                    val rc = buffer.read(sink, limit)
                    count++
                    rc
                }
            }
        }
        val byteStream = object : ByteStream.SourceStream() {
            override fun readFrom(): SdkSource = source
        }
        testCalculateHashesWithStream(byteStream)
    }
}

/**
 * Calculates a rolling sum for a hash. In this algorithm:
 * * hash size = chunk size
 * * every [update] call adds the positional input bytes to a rolling hash plus 1 (differentiates full from tree hashes)
 */
class RollingSumHashFunction(private val chunkSize: Int) : HashFunction {
    private val rollingHash = ByteArray(chunkSize)

    override val blockSizeBytes: Int = chunkSize
    override val digestSizeBytes: Int = chunkSize

    override fun digest(): ByteArray = rollingHash
    override fun reset() = fail("reset should not have been called")
    override fun update(input: ByteArray, offset: Int, length: Int) {
        assertEquals(chunkSize, input.size, "Chunk size must be exactly $chunkSize")
        for (i in input.indices) {
            rollingHash[i] = (rollingHash[i] + input[i] + 1).toByte()
        }
    }
}
