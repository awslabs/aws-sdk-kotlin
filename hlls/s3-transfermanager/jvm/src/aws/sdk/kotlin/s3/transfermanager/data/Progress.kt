package aws.sdk.kotlin.s3.transfermanager.data

/**
 * class recording total workload and current finished work progress
 */
data class Progress(
    /**
     * total number of files to transfer in an operation
     */
    val totalFilesToTransfer: Long,

    /**
     * total number of files' bytes to transfer in one operation
     */
    val totalBytesToTransfer: Long,

    /**
     * total number of split chunks to transfer in one operation
     */
    val totalChunksToTransfer: Long,

    /**
     * current number of transferred files in one operation
     */
    val filesTransferred: Long,

    /**
     * current number of transferred files' bytes in one operation
     */
    val bytesTransferred: Long,

    /**
     * current number of transferred split chunks in one operation
     */
    val chunksTransferred: Long,

    /**
     * indicator of whether this transfer is completed
     */
    val isDone: Boolean
)
