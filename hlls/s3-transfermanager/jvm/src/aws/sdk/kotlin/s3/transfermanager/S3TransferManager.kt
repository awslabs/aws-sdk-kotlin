package aws.sdk.kotlin.s3.transfermanager

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.s3.transfermanager.handler.Operation
import aws.sdk.kotlin.s3.transfermanager.listener.ProgressListener
import aws.sdk.kotlin.services.s3.S3Client

/**
 * Interface defining kotlin version S3 Transfer Manager and its functionalities including
 * 1) upload/download files/directories between S3 and local file system,
 * 2) copy files inside S3
 * 3) resume paused file transfer
 */
public interface S3TransferManager {

    public val config: Config

    public companion object {
        public operator fun invoke(block: Config.Builder.() -> Unit): S3TransferManager {
            val config = Config.Builder().apply(block).build()
            return DefaultS3TransferManager(config)
        }

        public operator fun invoke(config: Config): S3TransferManager = DefaultS3TransferManager(config)

        public fun fromEnvironment(block: (Config.Builder.() -> Unit)? = null): S3TransferManager {
            val builder = Config.Builder()
            if (block != null) builder.apply(block)
            return DefaultS3TransferManager(builder.build())
        }
    }

    public class Config private constructor(builder: Builder) {
        public val s3: S3Client = requireNotNull(builder.s3) { "s3 client is a required field" }

        public val chunkSize: Long = builder.chunkSize

        public companion object {
            public inline operator fun invoke(block: Builder.() -> Unit): Config = Builder().apply(block).build()
        }

        public class Builder {
            public var s3: S3Client? = null

            public var chunkSize: Long = 8000000

            @PublishedApi
            internal fun build(): Config = Config(this)
        }
    }

    /**
     * from local file system, upload a single file or directory to given S3Uri's bucket and key
     * can optionally pass progressListener to listen to transfer progress
     * IllegalArgumentException will be thrown if from path is invalid
     */
//    context(CoroutineContext)
    public suspend fun upload(
        from: String,
        to: S3Uri,
        progressListener: ProgressListener? = null // can set a default value to Null
    ): Operation

    /**
     * from S3, download a single object or key-prefix to given local path
     * can optionally pass progressListener to listen to transfer progress
     * IllegalArgumentException will be thrown if from uri or to path is invalid
     */
//    context(CoroutineScope) // context receiver
    public suspend fun download(
        from: S3Uri,
        to: String,
        progressListener: ProgressListener?
    ): Operation

    /**
     * copy a list of S3 object/key-prefixes to given S3Uri
     * can optionally pass progressListener to listen to transfer progress
     * IllegalArgumentException will be thrown if any from/to path is invalid
     */
//    context(CoroutineScope) // context receiver
    public suspend fun copy(
        from: List<S3Uri>,
        to: S3Uri,
        progressListener: ProgressListener?
    ): Operation

//    fun resume(pausedOperation: PausedOperation, progressListener: ProgressListener?): Operation

// Companion object
}
