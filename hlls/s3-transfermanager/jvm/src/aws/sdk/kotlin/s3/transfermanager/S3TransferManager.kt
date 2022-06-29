/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import aws.sdk.kotlin.s3.transfermanager.data.S3Uri
import aws.sdk.kotlin.s3.transfermanager.handler.Operation
import aws.sdk.kotlin.s3.transfermanager.listener.ProgressListener

/**
 * Just a placeholder for now...full interface coming soon!
 */
public interface S3TransferManager {
    // methods for file processing
//    context(CoroutineContext)
    public suspend fun upload(
        from: String,
        to: S3Uri,
        progressListener: ProgressListener?= null // can set a default value to Null
    ): Operation

//    context(CoroutineScope) // context receiver
//    fun upload(
//        from: List<String>,
//        to: S3Uri,
//        progressListener: ProgressListener?
//    ): Operation

//    context(CoroutineScope) // context receiver
//    fun download(
//        from: S3Uri,
//        to: String,
//        progressListener: ProgressListener?
//    ): Operation

//    context(CoroutineScope) // context receiver
//    fun download(
//        from: List<S3Uri>,
//        to: String,
//        progressListener: ProgressListener?
//    ): Operation

//    context(CoroutineScope) // context receiver
//    fun copy(
//        from: List<S3Uri>,
//        to: S3Uri,
//        progressListener: ProgressListener?
//    ): Operation

//    fun resume(pausedOperation: PausedOperation, progressListener: ProgressListener?): Operation

// Companion object
}
