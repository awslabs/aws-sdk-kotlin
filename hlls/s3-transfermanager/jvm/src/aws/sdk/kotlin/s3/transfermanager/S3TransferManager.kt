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


        // judge whether fileName is a single file or directory with some Java method
        // like Path.toFile().isDirectory() or File.isDirectory(path)

        // before actual upload, first traverse the directory to calculate total files/chunks/Bytes to be uploaded
        // and pass them to  Operation's Progress class
        // an upload() is listened by single Operation

        // for single file upload, generate multiple parallel PUT requests according to s3Path and send to S3 Client object
        // wait the S3 client to reply upload response, then use operator to listen to progress and control pausing and resuming

        // for directory, just use double pointer to start from fileDirectory/s3Path and recursively traverse directory/path
        // and call upload() to recursively finish the directory upload level by level


//    context(CoroutineScope) // context receiver
//    fun upload(
//        from: List<String>,
//        to: S3Uri,
//        progressListener: ProgressListener?
//    ): Operation {
        // iterate through fileList, for each String, call previous upload() to
        // distinguish whether it is a file name or directory and choose to upload correspondingly

        // ideal result is like this
        // Files system:
        // /foo/bar/dir1/file1
        // /foo/bar/dir1/file2
        // /foo/bar/dir1/file3
        // /foo/buzz/dir1/file1
        // /foo/buzz/dir1/file2
        // /foo/buzz/dir1/file3
        //
        // S3:
        // bucket/path/to/a/key
        // bucket/path/to/a/key/bar/dir1/file1
        // bucket/path/to/a/key/bar/dir1/file2
        // bucket/path/to/a/key/bar/dir1/file3
        // bucket/path/to/a/key/buzz/dir1/file1
        // bucket/path/to/a/key/buzz/dir1/file2
        // bucket/path/to/a/key/buzz/dir1/file3
//    }

//    context(CoroutineScope) // context receiver
//    fun download(
//        from: S3Uri,
//        to: String,
//        progressListener: ProgressListener?
//    ): Operation {
//        // in S3 system, try to use S3.HeadObject(s3Path.key) first to see whether or not there is
//        // a real key object there, if not, we know it is some key-prefix directory and can call
//        // s3.ListObjects(s3Path.keyPrefix) to get a List of object with such key-prefix and
//        // then try to re-serialize the tree directory locally
//        // (may need to add '/' or '\' at the end of key-prefix to limit it's precision)
//
//        // before actual upload, first traverse the s3Path to calculate total files/chunks/Bytes to be uploaded
//        // and pass them to  Operation's Progress class
//        // a download() is listened by single Operation
//
//        // for single file download, generate GET request from input parameter
//        // in S3, go to s3Path to find S3 object
//        // wait for S3 client to send back response and streamed data
//        // convert data to file and save locally under filePath, then use operator to listen to progress and control pausing and resuming
//
//        // for file directory, in S3, go to s3Path directory, locally go to fileDirectory
//        // first strip off the bottom key-prefix directory, create that folder under current filePath
//        // then iterate through key-prefix's all sub-file via s3.ListObjects() and call download() in each loop
//        // to recursively execute single object download or sub-directory download at local next level
//        // use operator to listen to progress and control pausing and resuming
//    }
//
//    context(CoroutineScope) // context receiver
//    fun download(
//        from: List<S3Uri>,
//        to: String,
//        progressListener: ProgressListener?
//    ): Operation {
//        // iterate through s3Paths, for each s3Path, pass to download() above
//        // and let it distinguish whether it is an object or key-prefix and implement download
//        // as well as listening to progress and controlling pausing and resuming
//    }
//
//    context(CoroutineScope) // context receiver
//    fun copy(
//        from: List<S3Uri>,
//        to: S3Uri,
//        progressListener: ProgressListener?
//    ): Operation {
//        // this is a non-stream operation, in Java TM design, I only see PUT request to S3 client
//        // iterate through fromPaths and toPaths correspondingly, for each pair of path
//        // in S3, send PUT/GET request to S3 client and operate like upload()-download()
//        // then after receiving S3 client's response, use operator to listen to copy progress
//    }
//
//    fun resume(pausedOperation: PausedOperation, progressListener: ProgressListener?): Operation
//
//// Companion object
}
