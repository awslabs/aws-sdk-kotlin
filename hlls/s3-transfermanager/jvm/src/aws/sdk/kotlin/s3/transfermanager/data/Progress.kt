package aws.sdk.kotlin.s3.transfermanager.data

/**
 * class recording total workload and current finished work progress
 */
public data class Progress(
    /**
     * total number of files to transfer in an operation
     */
    internal var totalFilesToTransfer: Long = 0,

    /**
     * total number of files' bytes to transfer in one operation
     */
    internal var totalBytesToTransfer: Long = 0,

    /**
     * total number of split chunks to transfer in one operation
     */
    internal var totalChunksToTransfer: Long = 0,

    /**
     * current number of transferred files in one operation
     */
    internal var filesTransferred: Long = 0,

    /**
     * current number of transferred files' bytes in one operation
     */
    internal var bytesTransferred: Long = 0,

    /**
     * current number of transferred split chunks in one operation
     */
    internal var chunksTransferred: Long = 0,

    /**
     * indicator of whether this transfer is completed
     */
    internal var isDone: Boolean = false,
) {
    /**
     * copy a new identical Progress from current one
     */
    public fun copy(): Progress {
        val newProgress = Progress()
        newProgress.totalFilesToTransfer = totalFilesToTransfer
        newProgress.totalBytesToTransfer = totalBytesToTransfer
        newProgress.totalChunksToTransfer = totalChunksToTransfer
        newProgress.filesTransferred = filesTransferred
        newProgress.bytesTransferred = bytesTransferred
        newProgress.chunksTransferred = chunksTransferred
        newProgress.isDone = isDone
        return newProgress
    }

    public fun estimateCorrect(progress: Progress): Boolean =
        totalFilesToTransfer == progress.totalFilesToTransfer && totalBytesToTransfer == progress.totalBytesToTransfer && totalChunksToTransfer == progress.totalChunksToTransfer

    public fun progressFinish(): Boolean =
        totalFilesToTransfer == filesTransferred && totalBytesToTransfer == bytesTransferred && totalChunksToTransfer == chunksTransferred && isDone
}
