package aws.sdk.kotlin.s3.transfermanager.listener

import aws.sdk.kotlin.s3.transfermanager.data.Progress

interface ProgressListener {
    fun onProgress(progress: Progress)
}