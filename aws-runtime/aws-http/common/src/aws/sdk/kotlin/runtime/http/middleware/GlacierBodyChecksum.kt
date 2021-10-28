package aws.sdk.kotlin.runtime.http.middleware

import aws.smithy.kotlin.runtime.http.Feature
import aws.smithy.kotlin.runtime.http.FeatureKey
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpClientFeatureFactory
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.util.Sha256
import aws.smithy.kotlin.runtime.util.encodeToHex
import aws.smithy.kotlin.runtime.util.sha256
import kotlinx.coroutines.flow.*
import kotlin.math.min

@Suppress("UNUSED_PARAMETER")
public class GlacierBodyChecksum(config: Config) : Feature {
    public class Config

    public companion object Feature : HttpClientFeatureFactory<Config, GlacierBodyChecksum> {
        override val key: FeatureKey<GlacierBodyChecksum> = FeatureKey("GlacierBodyChecksum")

        override fun create(block: Config.() -> Unit): GlacierBodyChecksum {
            val config = Config().apply(block)
            return GlacierBodyChecksum(config)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.finalize.intercept { req, next ->
            val body = req.subject.body
            require(body !is HttpBody.Streaming || body.isReplayable) {
                "This operation requires a byte array or replayable stream"
            }
            val hashes = body.calculateHashes()
            req.subject.headers.apply {
                set("X-Amz-Content-Sha256", hashes.sha256Full.encodeToHex())
                set("X-Amz-Sha256-Tree-Hash", hashes.sha256Tree.encodeToHex())
            }

            next.call(req)
        }
    }
}

private val chunkSizeBytes = 1024 * 1024 // 1MB

private class GlacierHashes(val sha256Full: ByteArray, val sha256Tree: ByteArray)

private suspend fun HttpBody.calculateHashes(): GlacierHashes {
    val full = Sha256()
    val hashTree = ArrayDeque<ByteArray>()

    chunks().collect { chunk ->
        full.update(chunk)
        hashTree.addLast(chunk.sha256())
    }

    if (hashTree.isEmpty()) {
        // Edge case for empty bodies
        hashTree.add(byteArrayOf().sha256())
    }

    while (hashTree.size > 1) {
        val nextRow = mutableListOf<ByteArray>()
        while (hashTree.isNotEmpty()) {
            val hash = Sha256()
            hashTree.removeFirst().let(hash::update)
            hashTree.removeFirstOrNull()?.let(hash::update)
            nextRow.add(hash.digest())
        }
        hashTree.addAll(nextRow)
    }

    return GlacierHashes(full.digest(), hashTree.first())
}

private suspend fun HttpBody.chunks(): Flow<ByteArray> = when (this) {
    is HttpBody.Empty -> flowOf()

    is HttpBody.Bytes -> {
        val size = bytes().size
        val chunkStarts = 0 until size step chunkSizeBytes
        val chunkRanges = chunkStarts.map { it until min(it + chunkSizeBytes, size) }
        chunkRanges.asFlow().map(bytes()::sliceArray)
    }

    is HttpBody.Streaming -> flow {
        val channel = readFrom()
        while (!channel.isClosedForRead) {
            emit(channel.readRemaining(chunkSizeBytes))
        }
        reset()
    }
}
