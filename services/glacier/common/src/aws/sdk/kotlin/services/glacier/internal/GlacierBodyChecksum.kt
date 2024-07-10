/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.glacier.internal

import aws.sdk.kotlin.services.glacier.model.GlacierException
import aws.smithy.kotlin.runtime.client.operationName
import aws.smithy.kotlin.runtime.hashing.Sha256
import aws.smithy.kotlin.runtime.http.operation.ModifyRequestMiddleware
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.http.operation.SdkHttpRequest
import aws.smithy.kotlin.runtime.http.request.headers
import aws.smithy.kotlin.runtime.text.encoding.encodeToHex

private const val DEFAULT_CHUNK_SIZE_BYTES = 1024 * 1024 // 1MB

internal class GlacierBodyChecksum(
    private val treeHasher: TreeHasher = TreeHasherImpl(DEFAULT_CHUNK_SIZE_BYTES, ::Sha256),
) : ModifyRequestMiddleware {
    override fun install(op: SdkHttpOperation<*, *>) {
        // after signing
        op.execution.receive.register(this)
    }

    override suspend fun modifyRequest(req: SdkHttpRequest): SdkHttpRequest {
        val body = req.subject.body
        if (body.isOneShot) {
            val opName = req.context.operationName ?: "This operation"
            throw GlacierException("$opName requires a byte array or replayable stream")
        }
        val hashes = treeHasher.calculateHashes(body)
        req.subject.headers {
            set("X-Amz-Content-Sha256", hashes.fullHash.encodeToHex())
            set("X-Amz-Sha256-Tree-Hash", hashes.treeHash.encodeToHex())
        }

        return req
    }
}
