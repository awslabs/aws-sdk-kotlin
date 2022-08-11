package aws.sdk.kotlin.s3.transfermanager.listener

import aws.sdk.kotlin.s3.transfermanager.data.Progress

/**
 * listener from user code that can be passed to S3TransferManager to monitor the transfer progress
 */
public interface ProgressListener {
    public fun onProgress(progress: Progress)
}
