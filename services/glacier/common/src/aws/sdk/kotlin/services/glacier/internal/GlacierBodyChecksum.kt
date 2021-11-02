/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.services.glacier.internal

import aws.sdk.kotlin.services.glacier.model.GlacierException
import aws.smithy.kotlin.runtime.client.operationName
import aws.smithy.kotlin.runtime.http.Feature
import aws.smithy.kotlin.runtime.http.FeatureKey
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpClientFeatureFactory
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.http.request.headers
import aws.smithy.kotlin.runtime.util.Sha256
import aws.smithy.kotlin.runtime.util.encodeToHex

private const val defaultChunkSizeBytes = 1024 * 1024 // 1MB

internal class GlacierBodyChecksum(config: Config) : Feature {
    private val treeHasher = config.treeHasher

    internal class Config {
        internal var treeHasher: TreeHasher = TreeHasherImpl(defaultChunkSizeBytes) { Sha256() }
    }

    internal companion object Feature : HttpClientFeatureFactory<Config, GlacierBodyChecksum> {
        override val key: FeatureKey<GlacierBodyChecksum> = FeatureKey("GlacierBodyChecksum")

        override fun create(block: Config.() -> Unit): GlacierBodyChecksum {
            val config = Config().apply(block)
            return GlacierBodyChecksum(config)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.finalize.intercept { req, next ->
            val body = req.subject.body
            if (body is HttpBody.Streaming && !body.isReplayable) {
                val opName = req.context.operationName ?: "This operation"
                throw GlacierException("$opName requires a byte array or replayable stream")
            }
            val hashes = treeHasher.calculateHashes(body)
            req.subject.headers {
                set("X-Amz-Content-Sha256", hashes.fullHash.encodeToHex())
                set("X-Amz-Sha256-Tree-Hash", hashes.treeHash.encodeToHex())
            }

            next.call(req)
        }
    }
}
