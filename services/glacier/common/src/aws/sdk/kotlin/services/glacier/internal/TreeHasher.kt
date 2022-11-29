/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.glacier.internal

import aws.smithy.kotlin.runtime.hashing.HashSupplier
import aws.smithy.kotlin.runtime.hashing.hash
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.io.buffer
import kotlinx.coroutines.flow.*
import kotlin.math.min

/**
 * The result of a [TreeHasher] calculation.
 * @param fullHash A full hash of the entire byte array (taken at once)
 * @param treeHash A composite hash of the byte array, taken in chunks.
 */
internal class Hashes(public val fullHash: ByteArray, public val treeHash: ByteArray)

/**
 * A hash calculator that returns [Hashes] derived using a tree. See
 * [Computing Checksums](https://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html) in the Glacier
 * service guide for more details.
 */
internal interface TreeHasher {
    /**
     * Perform the hash calculation.
     * @param body The [HttpBody] over which to calculate hashes.
     * @return A [Hashes] containing the results of the calculation.
     */
    suspend fun calculateHashes(body: HttpBody): Hashes
}

/**
 * The default implementation of a [TreeHasher].
 */
internal class TreeHasherImpl(private val chunkSizeBytes: Int, private val hashSupplier: HashSupplier) : TreeHasher {
    override suspend fun calculateHashes(body: HttpBody): Hashes {
        val full = hashSupplier()
        val hashTree = ArrayDeque<ByteArray>()

        body.chunks().collect { chunk ->
            full.update(chunk)
            hashTree.addLast(chunk.hash(hashSupplier))
        }

        if (hashTree.isEmpty()) {
            // Edge case for empty bodies
            hashTree.add(byteArrayOf().hash(hashSupplier))
        }

        while (hashTree.size > 1) {
            val nextRow = mutableListOf<ByteArray>()

            while (hashTree.isNotEmpty()) {
                if (hashTree.size == 1) {
                    nextRow.add(hashTree.removeFirst())
                } else {
                    val hash = hashSupplier()
                    hashTree.removeFirst().let(hash::update)
                    hashTree.removeFirst().let(hash::update)
                    nextRow.add(hash.digest())
                }
            }

            hashTree.addAll(nextRow)
        }

        return Hashes(full.digest(), hashTree.first())
    }

    private suspend fun HttpBody.chunks(): Flow<ByteArray> = when (this) {
        is HttpBody.Empty -> flowOf()

        is HttpBody.Bytes -> {
            val size = bytes().size
            val chunkStarts = 0 until size step chunkSizeBytes
            val chunkRanges = chunkStarts.map { it until min(it + chunkSizeBytes, size) }
            chunkRanges.asFlow().map(bytes()::sliceArray)
        }

        is HttpBody.ChannelContent -> flow {
            val channel = readFrom()
            val sink = SdkBuffer()
            while (!channel.isClosedForRead) {
                var remaining = chunkSizeBytes.toLong()
                while (remaining > 0L) {
                    val rc = channel.read(sink, remaining)
                    if (rc == -1L) break // channel closed before a full chunk could be read
                    remaining -= rc
                }
                emit(sink.readByteArray())
            }
        }
        is HttpBody.SourceContent -> flow {
            val source = readFrom().buffer()
            while (!source.exhausted()) {
                source.request(chunkSizeBytes.toLong())
                val limit = minOf(chunkSizeBytes.toLong(), source.buffer.size)
                emit(source.readByteArray(limit))
            }
        }
    }
}
