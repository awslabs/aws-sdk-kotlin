package aws.sdk.kotlin.s3.transfermanager.data

/**
 * class recording total workload and current finished work progress
 */
public data class Progress(
    /**
     * total number of files to transfer in an operation
     */
    public var totalFilesToTransfer: Long = 0,

    /**
     * total number of files' bytes to transfer in one operation
     */
    public var totalBytesToTransfer: Long = 0,

    /**
     * total number of split chunks to transfer in one operation
     */
    public var totalChunksToTransfer: Long = 0,

    /**
     * current number of transferred files in one operation
     */
    public var filesTransferred: Long = 0,

    /**
     * current number of transferred files' bytes in one operation
     */
    public var bytesTransferred: Long = 0,

    /**
     * current number of transferred split chunks in one operation
     */
    public var chunksTransferred: Long = 0,

    /**
     * indicator of whether this transfer is completed
     */
    public var isDone: Boolean = false,
)
