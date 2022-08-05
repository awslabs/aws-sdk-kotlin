package aws.sdk.kotlin.s3.transfermanager.data

/**
 * class recording total workload and current finished work progress
 */
public data class Progress(
    /**
     * total number of files to transfer in an operation
     */
    var totalFilesToTransfer: Long = 0,

    /**
     * total number of files' bytes to transfer in one operation
     */
    var totalBytesToTransfer: Long = 0,

    /**
     * total number of split chunks to transfer in one operation
     */
    var totalChunksToTransfer: Long = 0,

    /**
     * current number of transferred files in one operation
     */
    var filesTransferred: Long = 0,

    /**
     * current number of transferred files' bytes in one operation
     */
    var bytesTransferred: Long = 0,

    /**
     * current number of transferred split chunks in one operation
     */
    var chunksTransferred: Long = 0,

    /**
     * indicator of whether this transfer is completed
     */
    var isDone: Boolean = false
)
